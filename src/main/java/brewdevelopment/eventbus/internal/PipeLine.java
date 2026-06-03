package brewdevelopment.eventbus.internal;

import brewdevelopment.eventbus.event.Event;

import java.util.List;

public interface PipeLine {
    void execute(Event event);

    void fillFields(ErrorCallBack errorCallBack, List callees);
}
