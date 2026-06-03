package brewdevelopment.eventbus.internal;

import brewdevelopment.eventbus.EventListener;
import brewdevelopment.eventbus.event.Event;

public interface WrappedEventCaller extends EventListener<Event> {
    void call(Event event);

    @Override
    default void invoke(Event event) {
        call(event);
    }
}
