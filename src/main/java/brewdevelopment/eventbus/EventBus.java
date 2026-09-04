package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.*;
import brewdevelopment.eventbus.event.stats.EventStats;
import brewdevelopment.eventbus.internal.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A high-performance ASM backed implementation of {@link IEventBus}.
 */
@SuppressWarnings({"unused", "unchecked"})
public final class EventBus implements IEventBus {

    public EventBus() {
        this.config = Configuration.DEFAULT;
    }

    public EventBus(@NotNull Configuration config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    private final Configuration config;

    private final Map<Class<? extends Event>, List<RegisteredListener<?>>> listeners = new ConcurrentHashMap<>();
    private final Map<Class<? extends Event>, PipeLine> pipelines = new ConcurrentHashMap<>();
    private final Map<Class<? extends Event>, EventStats> eventStats = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Class<?>>> hierarchyCache = new ConcurrentHashMap<>();

    private final List<Predicate<ListenerContext<?>>> dispatchFilters = new CopyOnWriteArrayList<>();
    private final ThreadLocal<Event> currentEvent = new ThreadLocal<>();
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread thread = new Thread(r, "EventBus-Async-Thread");
                thread.setDaemon(true);
                return thread;
            }
    );
    private ExceptionHandler exceptionHandler = (event, listener, exception) -> exception.printStackTrace();
    private final ErrorCallBack errorCallBack = throwable -> {
        Event event = currentEvent.get();
        if (event != null) {
            exceptionHandler.handle(event, null, throwable);
        } else {
            throwable.printStackTrace();
        }
    };

    @Override
    public <E extends Event> void subscribe(
            Class<E> eventType,
            EventListener<E> listener,
            Object owner,
            Object container,
            int priority,
            boolean async
    ) {
        if (async) {
            if (CancellableEvent.class.isAssignableFrom(eventType)) {
                throw new IllegalArgumentException("Cannot subscribe an asynchronous listener to a CancellableEvent: " + eventType.getName());
            }
            if (MutableEventValue.class.isAssignableFrom(eventType)) {
                throw new IllegalArgumentException("Cannot subscribe an asynchronous listener to a MutableEventValue: " + eventType.getName());
            }
        }

        WrappedEventCaller caller;
        if (listener instanceof WrappedEventCaller) {
            caller = (WrappedEventCaller) listener;
        } else {
            caller = event -> listener.invoke((E) event);
        }

        if (async) {
            WrappedEventCaller syncCaller = caller;
            caller = event -> asyncExecutor.submit(() -> syncCaller.call(event));
        }

        List<RegisteredListener<?>> typeListeners = listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
        typeListeners.add(new RegisteredListener<>(eventType, (EventListener<E>) caller, listener, owner, container, priority, async, config.recordStats()));

        typeListeners.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        pipelines.clear();
    }

    @Override
    public void register(Object container, Object owner) {
        Class<?> clazz = (container instanceof Class<?>) ? (Class<?>) container : container.getClass();
        boolean isStaticOnly = container instanceof Class<?>;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                if (method.getParameterCount() != 1) {
                    System.err.println("Method " + method.getName() + " in class " + clazz.getName() + " is annotated with @Subscribe but does not have exactly one parameter.");
                    continue;
                }

                if (isStaticOnly && !Modifier.isStatic(method.getModifiers())) {
                    System.err.println("Method " + method.getName() + " in class " + clazz.getName() + " is annotated with @Subscribe but is not static, while the container is a Class.");
                    continue;
                }

                Class<?> paramType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(paramType)) {
                    continue;
                }

                Class<Event> eventType = (Class<Event>) paramType;
                Subscribe annotation = method.getAnnotation(Subscribe.class);

                method.setAccessible(true);
                WrappedEventCaller caller = CallerGenerator.generate(container, method, eventType, config);
                subscribe(eventType, caller, owner, isStaticOnly ? null : container, annotation.priority(), annotation.async());
            }
        }
    }

    @Contract("_ -> param1")
    @Override
    public <E extends Event> @NotNull E post(@NotNull E event) {
        Class<? extends Event> eventClass = event.getClass();

        if (dispatchFilters.isEmpty()) {
            currentEvent.set(event);
            try {
                PipeLine pipeline = pipelines.computeIfAbsent(eventClass, this::rebuildPipeline);
                pipeline.execute(event);
            } finally {
                currentEvent.remove();
            }
        } else {
            slowPost(event);
        }

        return event;
    }

    private PipeLine rebuildPipeline(Class<? extends Event> eventClass) {
        List<RegisteredListener<?>> applicable = new ArrayList<>();
        List<Class<?>> hierarchy = getHierarchy(eventClass);
        for (Class<?> clazz : hierarchy) {
            List<RegisteredListener<?>> typeListeners = listeners.get(clazz);
            if (typeListeners != null) {
                applicable.addAll(typeListeners);
            }
        }
        applicable.sort((a, b) -> Integer.compare(b.priority(), a.priority()));

        EventStats stats = config.recordStats()
                ? eventStats.computeIfAbsent(eventClass, k -> new EventStats())
                : EventStats.noOp();
        return PipelineGenerator.generate(eventClass, applicable, errorCallBack, stats, config);
    }

    private List<Class<?>> getHierarchy(Class<?> eventClass) {
        return hierarchyCache.computeIfAbsent(eventClass, clazz -> {
            List<Class<?>> hierarchy = new ArrayList<>();
            collectHierarchy(clazz, hierarchy);
            return Collections.unmodifiableList(hierarchy);
        });
    }

    private void collectHierarchy(Class<?> clazz, List<Class<?>> hierarchy) {
        if (clazz == null || !Event.class.isAssignableFrom(clazz) || hierarchy.contains(clazz)) {
            return;
        }
        hierarchy.add(clazz);
        for (Class<?> iface : clazz.getInterfaces()) {
            collectHierarchy(iface, hierarchy);
        }
        collectHierarchy(clazz.getSuperclass(), hierarchy);
    }

    private <E extends Event> void slowPost(@NotNull E event) {
        long start = System.nanoTime();
        Class<? extends Event> eventClass = event.getClass();

        List<RegisteredListener<?>> all = new ArrayList<>();
        List<Class<?>> hierarchy = getHierarchy(eventClass);
        for (Class<?> clazz : hierarchy) {
            List<RegisteredListener<?>> typeListeners = listeners.get(clazz);
            if (typeListeners != null) all.addAll(typeListeners);
        }
        all.sort((a, b) -> Integer.compare(b.priority(), a.priority()));

        for (RegisteredListener<?> raw : all) {
            RegisteredListener<E> listener = (RegisteredListener<E>) raw;
            ListenerContext<E> context = new ListenerContext<>(listener.listener(), event, listener.owner(), listener.container());

            boolean rejected = false;
            for (Predicate<ListenerContext<?>> filter : dispatchFilters) {
                if (filter.test(context)) {
                    rejected = true;
                    break;
                }
            }

            if (rejected) continue;

            if (listener.async()) {
                asyncExecutor.submit(() -> invokeListener(event, listener));
            } else {
                invokeListener(event, listener);
                if (event instanceof CancellableEvent ce && ce.isCancelled()) break;
            }
        }

        if (config.recordStats()) {
            eventStats.computeIfAbsent(eventClass, k -> new EventStats()).record(System.nanoTime() - start);
        }
    }

    private <E extends Event> void invokeListener(E event, RegisteredListener<E> listener) {
        long handlerStart = System.nanoTime();
        try {
            listener.listener().invoke(event);
        } catch (Throwable t) {
            exceptionHandler.handle(event, listener, t);
        } finally {
            if (config.recordStats()) {
                listener.stats().record(System.nanoTime() - handlerStart);
            }
        }
    }

    @Override
    public void unsubscribe(EventListener<?> listener) {
        for (List<RegisteredListener<?>> typeListeners : listeners.values()) {
            typeListeners.removeIf(registered -> registered.originalListener().equals(listener));
        }
        pipelines.clear();
    }

    @Override
    public void unregister(Object owner) {
        for (List<RegisteredListener<?>> typeListeners : listeners.values()) {
            typeListeners.removeIf(registered -> registered.owner() != null && registered.owner().equals(owner));
        }
        pipelines.clear();
    }

    @Override
    public void addDispatchFilter(Predicate<ListenerContext<?>> filter) {
        dispatchFilters.add(filter);
        pipelines.clear();
    }

    @Override
    public void setExceptionHandler(ExceptionHandler handler) {
        this.exceptionHandler = handler;
    }

    @Override
    public EventStats getStats(Class<? extends Event> eventType) {
        if (!config.recordStats()) {
            return EventStats.noOp();
        }
        return eventStats.getOrDefault(eventType, EventStats.noOp());
    }

    @Contract(pure = true)
    @Override
    public @NotNull @UnmodifiableView Map<Class<? extends Event>, EventStats> getEventStats() {
        if (!config.recordStats()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(eventStats);
    }

    @Override
    public Collection<RegisteredListener<?>> getAllListeners() {
        return listeners.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    public int getRegisteredEventCount() {
        return listeners.size();
    }

    @Override
    public void shutdown() {
        asyncExecutor.shutdown();
    }
}
