package brewdevelopment.eventbus.event;

/**
 * An interface for events that can be cancelled.
 * <p>
 * Note: This type of event is incompatible with asynchronous listeners.
 */
public interface CancellableEvent extends Event {
    /**
     * @return true if the event has been cancelled
     */
    public boolean isCancelled();

    /**
     * Cancels the event.
     * @return true if the cancellation was successful
     */
    public boolean cancel();
}
