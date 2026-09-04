package brewdevelopment.eventbus.internal;

import brewdevelopment.eventbus.event.Event;

@FunctionalInterface
public interface PipeLine {
    void execute(Event event);
}
