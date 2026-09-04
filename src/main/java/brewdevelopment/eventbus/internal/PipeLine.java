package brewdevelopment.eventbus.internal;

import brewdevelopment.eventbus.event.Event;

import java.util.List;

@FunctionalInterface
public interface PipeLine {
    void execute(Event event);
}
