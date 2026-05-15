package brewdevelopment.eventbus.event;

import brewdevelopment.eventbus.RegisteredListener;

/**
 * A handler for exceptions thrown during event dispatch.
 */
@FunctionalInterface
public interface ExceptionHandler {
    /**
     * Handled an exception thrown by a listener
     *
     * @param event the event being dispatched
     * @param listener the listener that threw the exception
     * @param exception the exception thrown
     */
    void handle(Event event, RegisteredListener<?> listener, Throwable exception);
}
