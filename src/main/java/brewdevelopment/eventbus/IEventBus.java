package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.ExceptionHandler;
import brewdevelopment.eventbus.event.stats.EventStats;

import java.util.Collection;
import java.util.function.Predicate;

public interface IEventBus {

    /**
     * Subscribes a listener to a specific event type with default priority (0).
     *
     * @param eventType the class of the event to listen for
     * @param listener  the listener implementation
     * @param owner     the module owning this listener, used for bulk unsubscription
     * @param <E>       the event type
     */
    default <E extends Event> void subscribe(
            Class<E> eventType,
            EventListener<E> listener,
            Module owner
    ) {
        subscribe(eventType, listener, owner, 0);
    }

    /**
     * Subscribes a listener to a specific event type with priority
     *
     * @param eventType the class of the event to listen for
     * @param listener  the listener implementation
     * @param owner     the module owning this listener
     * @param priority  the priority of the listener (higher values called first)
     * @param <E>       the event type
     */
    <E extends Event> void subscribe(
            Class<E> eventType,
            EventListener<E> listener,
            Module owner,
            int priority
    );

    /**
     * Registers all methods marked with @Subscribe in the given container.
     *
     * @param container the object containing listener methods
     * @param owner     the module owning these listeners
     */
    void register(Object container, Module owner);

    /**
     * Dispatches an event to all registered listeners of its type and supertypes.
     *
     * @param event the event to post
     * @param <E>   the event type
     */
    <E extends Event> void post(E event);

    /**
     * Unsubscribes a specific listener instance.
     *
     * @param listener the listener to remove
     */
    void unsubscribe(EventListener<?> listener);

    /**
     * Removes all listeners owned by a specific module.
     *
     * @param owner the module whose listeners should be removed
     */
    void unsubscribeAll(Module owner);

    /**
     * Adds a global filter that can intercept or block event dispatch.
     *
     * @param filter the filter to add
     */
    void addDispatchFilter(Predicate<ListenerContext<?>> filter);

    /**
     * Sets the global handler for exceptions thrown by listeners.
     *
     * @param handler the exception handler
     */
    void setExceptionHandler(ExceptionHandler handler);

    /**
     * @param eventType the event type
     * @return the statistics for the given event type
     */
    EventStats getStats(Class<? extends Event> eventType);

    /**
     * @return all registered listeners
     */
    Collection<RegisteredListener<?>> getAllListeners();
}
