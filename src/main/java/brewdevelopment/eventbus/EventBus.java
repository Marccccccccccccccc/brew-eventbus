package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.ExceptionHandler;
import brewdevelopment.eventbus.event.Subscribe;
import brewdevelopment.eventbus.event.stats.EventStats;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A high-performance, thread-safe implementation of {@link IEventBus}.
 * <p>
 * This implementation uses a {@link ConcurrentHashMap} for listener storage and 
 * caches class hierarchies to optimize polymorphic event dispatching.
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public final class EventBus implements IEventBus {

    private final Map<Class<? extends Event>, List<RegisteredListener<?>>> listeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Class<?>>> hierarchyCache = new ConcurrentHashMap<>();
    private final Map<Class<? extends Event>, EventStats> eventStats = new ConcurrentHashMap<>();

    private final List<Predicate<ListenerContext<?>>> dispatchFilters = new CopyOnWriteArrayList<>();
    private ExceptionHandler exceptionHandler = (event, listener, exception) -> exception.printStackTrace();

    @Override
    public <E extends Event> void subscribe(
            Class<E> eventType,
            EventListener<E> listener,
            Module owner,
            int priority
    ) {
        List<RegisteredListener<?>> typeListeners = listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
        typeListeners.add(new RegisteredListener<>(eventType, listener, owner, priority));
        // Sort by priority (descending)
        typeListeners.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    }

    @Override
    public void register(Object container, Module owner) {
        for (Method method : container.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                if (method.getParameterCount() != 1) {
                    continue;
                }

                Class<?> paramType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(paramType)) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                Class<Event> eventType = (Class<Event>) paramType;
                Subscribe annotation = method.getAnnotation(Subscribe.class);

                method.setAccessible(true);
                EventListener<Event> listener = event -> {
                    try {
                        method.invoke(container, event);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke annotated listener", e);
                    }
                };

                subscribe(eventType, listener, owner, annotation.priority());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Event> void post(E event) {
        long start = System.nanoTime();
        Class<? extends Event> eventClass = (Class<? extends Event>) event.getClass();
        List<Class<?>> hierarchy = getHierarchy(eventClass);

        for (Class<?> clazz : hierarchy) {
            List<RegisteredListener<?>> typeListeners = listeners.get(clazz);
            if (typeListeners == null || typeListeners.isEmpty()) {
                continue;
            }

            for (RegisteredListener<?> raw : typeListeners) {
                RegisteredListener<E> listener = (RegisteredListener<E>) raw;

                if (!dispatchFilters.isEmpty()) {
                    ListenerContext<E> context = new ListenerContext<>(
                            listener.listener(),
                            event,
                            listener.owner()
                    );

                    boolean rejected = false;
                    for (Predicate<ListenerContext<?>> filter : dispatchFilters) {
                        if (filter.test(context)) {
                            rejected = true;
                            break;
                        }
                    }

                    if (rejected) {
                        continue;
                    }
                }

                long handlerStart = System.nanoTime();
                try {
                    listener.listener().invoke(event);
                } catch (Throwable t) {
                    exceptionHandler.handle(event, listener, t);
                } finally {
                    listener.stats().record(System.nanoTime() - handlerStart);
                }
            }
        }
        
        eventStats.computeIfAbsent(eventClass, k -> new EventStats())
                .record(System.nanoTime() - start);
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

    @Override
    public void unsubscribe(EventListener<?> listener) {
        for (List<RegisteredListener<?>> typeListeners : listeners.values()) {
            typeListeners.removeIf(registered -> registered.listener().equals(listener));
        }
    }

    @Override
    public void unsubscribeAll(Module owner) {
        for (List<RegisteredListener<?>> typeListeners : listeners.values()) {
            typeListeners.removeIf(registered -> registered.owner().equals(owner));
        }
    }

    @Override
    public void addDispatchFilter(
            Predicate<ListenerContext<?>> filter
    ) {
        dispatchFilters.add(filter);
    }

    @Override
    public void setExceptionHandler(ExceptionHandler handler) {
        this.exceptionHandler = handler;
    }

    @Override
    public EventStats getStats(Class<? extends Event> eventType) {
        return eventStats.getOrDefault(eventType, new EventStats());
    }

    @Override
    public Collection<RegisteredListener<?>> getAllListeners() {
        return listeners.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}
