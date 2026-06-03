package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.ExceptionHandler;
import brewdevelopment.eventbus.event.stats.EventStats;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The core interface for the event bus system.
 * Handles event subscription, dispatching, and filtering.
 */
public interface IEventBus {

    /**
     * Subscribes a listener to a specific event type with default priority (0).
     */
    default <E extends Event> void subscribe(Class<E> eventType, EventListener<E> listener) {
        subscribe(eventType, listener, null, null, 0, false);
    }

    /**
     * Subscribes a listener with an explicit owner for unregistration.
     */
    default <E extends Event> void subscribe(Class<E> eventType, EventListener<E> listener, Object owner) {
        subscribe(eventType, listener, owner, null, 0, false);
    }

    /**
     * Subscribes a listener with priority.
     */
    default <E extends Event> void subscribe(Class<E> eventType, EventListener<E> listener, Object owner, int priority) {
        subscribe(eventType, listener, owner, null, priority, false);
    }

    /**
     * Subscribes a listener with priority and async flag.
     */
    default <E extends Event> void subscribe(Class<E> eventType, EventListener<E> listener, Object owner, int priority, boolean async) {
        subscribe(eventType, listener, owner, null, priority, async);
    }

    /**
     * Full subscription method.
     */
    <E extends Event> void subscribe(Class<E> eventType, EventListener<E> listener, Object owner, Object container, int priority, boolean async);

    /**
     * Registers all methods marked with @Subscribe in the given container.
     * Uses the container itself as the owner.
     */
    default void register(Object container) {
        register(container, container);
    }

    /**
     * Registers all methods marked with @Subscribe with an explicit owner.
     */
    void register(Object container, Object owner);

    /**
     * Dispatches an event and returns it.
     */
    <E extends Event> E post(E event);

    /**
     * Unsubscribes a specific listener instance.
     */
    void unsubscribe(EventListener<?> listener);

    /**
     * Removes all listeners owned by a specific object.
     */
    void unregister(Object owner);

    /**
     * Adds a global filter.
     */
    void addDispatchFilter(Predicate<ListenerContext<?>> filter);

    /**
     * Sets the exception handler.
     */
    void setExceptionHandler(ExceptionHandler handler);

    /**
     * @return statistics for an event type
     */
    EventStats getStats(Class<? extends Event> eventType);

    /**
     * @return all event statistics
     */
    Map<Class<? extends Event>, EventStats> getEventStats();

    /**
     * @return all registered listeners
     */
    Collection<RegisteredListener<?>> getAllListeners();

    /**
     * @return the number of unique event types
     */
    int getRegisteredEventCount();

    /**
     * Shuts down async services.
     */
    void shutdown();
}
