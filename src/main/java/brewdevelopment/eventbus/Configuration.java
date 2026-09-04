package brewdevelopment.eventbus;

import lombok.Builder.Default;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.Builder;
import java.util.function.Supplier;

@SuppressWarnings("unused")
@Getter
@Accessors(fluent = true)
@Builder
public final class Configuration {
    public static final String DEFAULT_GENERATED_CLASS_NAME_PREFIX = "brewdevelopment/eventbus/generated/Pipeline_";
    public static final String DEFAULT_GENERATED_CALLER = "brewdevelopment/eventbus/generated/GeneratedCaller_";

    public static final Configuration DEFAULT = Configuration.builder().build();

    @Default
    private final Supplier<String> generatedClassNameSupplier = () -> DEFAULT_GENERATED_CLASS_NAME_PREFIX;
    @Default
    private final Supplier<String> generatedCallerSupplier = () -> DEFAULT_GENERATED_CALLER;
    @Default
    private final boolean recordStats = true;
    @Default
    private final boolean enableErrorCallbacks = false;
    @Default
    private final boolean warnOnInvalidListenerMethod = true;
}
