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
    private final Module owner;
    private final int priority;
    private final boolean async;
    private final HandlerStats stats = new HandlerStats();

    public RegisteredListener(
            Class<E> eventType,
            EventListener<E> listener,
            Module owner,
            int priority,
            boolean async
    ) {
        this.eventType = eventType;
        this.listener = listener;
        this.owner = owner;
        this.priority = priority;
        this.async = async;
    }

    public Class<E> eventType() {
        return eventType;
    }

    public EventListener<E> listener() {
        return listener;
    }

    public Module owner() {
        return owner;
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
