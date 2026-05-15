package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.Event;

/**
 * A functional interface for brewdevelopment.eventbus.event listeners.
 *
 * @param <E> the type of brewdevelopment.eventbus.event this listener handles
 */
@FunctionalInterface
public interface EventListener<E extends Event> {
    /**
     * Invoked when an brewdevelopment.eventbus.event of type E is posted.
     *
     * @param event the brewdevelopment.eventbus.event instance
     */
    void invoke(E event);
}