package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.stats.HandlerStats;

/**
 * Listener MetaData
 *
 * @param <E> the event type
 */
public record RegisteredListener<E extends Event>(
        Class<E> eventType, EventListener<E> listener,
        EventListener<E> originalListener, Object owner, Object container,
        int priority, boolean async, HandlerStats stats) {
    public RegisteredListener(
            Class<E> eventType,
            EventListener<E> listener,
            EventListener<E> originalListener,
            Object owner,
            Object container,
            int priority,
            boolean async,
            boolean recordStats
    ) {
        this(eventType, listener, originalListener, owner, container, priority, async, recordStats ? new HandlerStats() : HandlerStats.noOp());
    }
}
