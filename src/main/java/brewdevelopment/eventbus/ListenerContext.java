package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.Event;

/**
 * Contextual information for an event dispatch.
 * Used primarily by dispatch filters to make decisions.
 *
 * @param listener  the listener targeted for this dispatch
 * @param event     the event being dispatched
 * @param owner     the object that owns the listener
 * @param container the object containing the listener method
 * @param <E>       the event type
 */
public record ListenerContext<E extends Event>(
        EventListener<E> listener,
        E event,
        Object owner,
        Object container) {

}
