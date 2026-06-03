package brewdevelopment.eventbus.event;

/**
 * An interface for events that hold a value that can be modified by listeners.
 * <p>
 * Note: This type of event is incompatible with asynchronous listeners.
 *
 * @param <T> the type of the value
 */
public interface MutableEventValue<T> extends Event {
    /**
     * @return the current value
     */
    T value();

    /**
     * Sets a new value for the event.
     *
     * @param value the new value
     */
    void set(T value);
}
