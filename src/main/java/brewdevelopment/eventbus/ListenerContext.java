package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.Event;

/**
 * Contextual information for an event dispatch.
 * Used primarily by dispatch filters to make decisions.
 *
 * @param listener the listener targeted for this dispatch
 * @param event the brewdevelopment.eventbus.event being dispatched
 * @param owner the module that owns the listener
 * @param <E> the brewdevelopment.eventbus.event type
 */
public record ListenerContext<E extends Event>(
        EventListener<E> listener,
        E event,
        Module owner) {
}
