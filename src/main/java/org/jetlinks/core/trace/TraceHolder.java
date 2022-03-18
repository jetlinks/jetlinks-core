package org.jetlinks.core.trace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import reactor.core.publisher.Mono;

public class TraceHolder {

    private static String GLOBAL_APP_NAME = System.getProperty("trace.app.name", "default");
    private static boolean traceEnabled = Boolean.parseBoolean(System.getProperty("trace.enabled", "true"));

    public static void setupGlobalName(String name) {
        GLOBAL_APP_NAME = name;
    }

    public static String appName() {
        return GLOBAL_APP_NAME;
    }

    public static boolean isDisabled() {
        return !traceEnabled;
    }

    public static void enable() {
        traceEnabled = true;
    }

    public static void disable() {
        traceEnabled = false;
    }

    public static OpenTelemetry telemetry() {
        return GlobalOpenTelemetry.get();
    }

    public static <T> Mono<T> writeContextTo(T source, TextMapSetter<T> setter) {
        if (!traceEnabled) {
            return Mono.just(source);
        }
        ContextPropagators propagators = TraceHolder.telemetry().getPropagators();
        TextMapPropagator propagator = propagators.getTextMapPropagator();
        return Mono
                .deferContextual(ctx -> {
                    Context context = ctx.getOrDefault(Context.class, Context.root());
                    if (null != context) {
                        propagator.inject(context, source, setter);
                    }
                    return Mono.just(source);
                });
    }


}
