package brewdevelopment.eventbus;

import brewdevelopment.eventbus.event.Event;
import brewdevelopment.eventbus.event.stats.HandlerStats;

/**
 * Internal record-like class that keeps track of a subscribed listener and its metadata.
 *
 * @param <E> the event type
 */
public final class RegisteredListener<E extends Event> {

    private final Class<E> eventType;
    private final EventListener<E> listener;
    private final EventListener<E> originalListener;
    private final Object owner;
    private final Object container;
    private final int priority;
    private final boolean async;
    private final HandlerStats stats = new HandlerStats();

    public RegisteredListener(
            Class<E> eventType,
            EventListener<E> listener,
            EventListener<E> originalListener,
            Object owner,
            Object container,
            int priority,
            boolean async
    ) {
        this.eventType = eventType;
        this.listener = listener;
        this.originalListener = originalListener;
        this.owner = owner;
        this.container = container;
        this.priority = priority;
        this.async = async;
    }

    public Class<E> eventType() {
        return eventType;
    }

    public EventListener<E> listener() {
        return listener;
    }

    public EventListener<E> originalListener() {
        return originalListener;
    }

    public Object owner() {
        return owner;
    }

    public Object container() {
        return container;
    }

    public int priority() {
        return priority;
    }

    public boolean async() {
        return async;
    }

    /**
     * @return the performance statistics for this listener
     */
    public HandlerStats stats() {
        return stats;
    }
}
