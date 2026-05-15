package brewdevelopment.eventbus.event;
/**
 * An interface for events that hold a value that can be modified by listeners.
 *
 * @param <T> the type of the value
 */
public interface MutableEventValue<T> extends Event {
    /**
     * @return the current value of the brewdevelopment.eventbus.event
     */
    public T value();
}

