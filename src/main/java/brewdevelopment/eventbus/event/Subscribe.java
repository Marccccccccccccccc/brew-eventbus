package brewdevelopment.eventbus.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event listener.
 * The method must have exactly one parameter that extends {@link Event}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
    /**
     * The priority of the listener. Higher values are called first.
     * @return the priority
     */
    int priority() default 0;

    /**
     * Whether the listener should be invoked asynchronously.
     * @return true if async
     */
    boolean async() default false;
}