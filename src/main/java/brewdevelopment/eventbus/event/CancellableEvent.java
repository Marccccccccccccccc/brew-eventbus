package brewdevelopment.eventbus.event;

/**
 * An interface for events that can be cancelled.
 * When an brewdevelopment.eventbus.event is cancelled, it typically indicates that the action
 * associated with the brewdevelopment.eventbus.event should be aborted.
 */
public interface CancellableEvent extends Event {
    /**
     * @return true if the brewdevelopment.eventbus.event has been cancelled
     */
    public boolean isCancelled();

    /**
     * Cancels the brewdevelopment.eventbus.event.
     * @return true if the cancellation was successful
     */
    public boolean cancel();
}

