package org.jetlinks.core.trace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.apache.commons.collections.MapUtils;
import org.jetlinks.core.topic.Topic;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.function.Consumer3;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class TraceHolder {

    private static String GLOBAL_APP_NAME = System.getProperty("trace.app.name", "default");
    private static boolean traceEnabled = Boolean.parseBoolean(System.getProperty("trace.enabled", "true"));

    private static final Topic<String> disabledSpanName = Topic.createRoot();
    private static final Topic<String> enabledSpanName = Topic.createRoot();

    public static void setupGlobalName(String name) {
        GLOBAL_APP_NAME = name;
    }

    public static String appName() {
        return GLOBAL_APP_NAME;
    }

    public static boolean isEnabled() {
        return traceEnabled;
    }

    public static boolean isDisabled() {
        return !isEnabled();
    }

    public static boolean isEnabled(String name) {
        //全局关闭
        if (!traceEnabled) {
            return false;
        }
        AtomicReference<Boolean> enabled = new AtomicReference<>();
        //获取启用的span
        enabledSpanName
                .findTopic(name,
                           topic -> enabled.set(true),
                           () -> {
                           });
        if (enabled.get() == null) {
            //获取禁用的span
            disabledSpanName
                    .findTopic(name,
                               topic -> enabled.set(false),
                               () -> {
                               });
        }
        if (enabled.get() == null) {
            return true;
        }

        return enabled.get();
    }

    public static boolean isDisabled(String name) {
        return !isEnabled(name);
    }

    public static void enable() {
        traceEnabled = true;
    }

    public static void disable() {
        traceEnabled = false;
    }

    public static Disposable enable(String spanName, String handler) {
        disabledSpanName
                .getTopic(spanName)
                .ifPresent(topic -> topic.unsubscribe(handler));
        enabledSpanName
                .append(spanName)
                .subscribe(handler);
        return () -> disable(spanName, handler);
    }

    public static void disable(String spanName, String handler) {
        disabledSpanName
                .append(spanName)
                .subscribe(handler);
        enabledSpanName
                .getTopic(spanName)
                .ifPresent(topic -> topic.unsubscribe(handler));
    }

    public static OpenTelemetry telemetry() {
        return GlobalOpenTelemetry.get();
    }


    public static <T> reactor.util.context.Context readToContext(ContextView ctx,
                                                                 Map<String, ?> getter) {
        if (TraceHolder.isDisabled() || MapUtils.isEmpty(getter)) {
            return reactor.util.context.Context.of(ctx);
        }
        return readToContext(ctx, getter, MapTextMapGetter.instance());
    }

    public static <T> reactor.util.context.Context readToContext(ContextView ctx,
                                                                 T source,
                                                                 TextMapGetter<T> getter) {
        ContextPropagators propagators = TraceHolder.telemetry().getPropagators();
        TextMapPropagator propagator = propagators.getTextMapPropagator();

        Context context = ctx.getOrDefault(Context.class, Context.root());
        if (null != context) {
            context = propagator.extract(context, source, getter);
            return reactor.util.context.Context
                    .of(Context.class, context);
        }
        return reactor.util.context.Context.of(ctx);
    }

    public static <T> T writeContextTo(ContextView ctx,
                                       T source,
                                       Consumer3<T, String, String> setter) {
        if (!traceEnabled) {
            return source;
        }
        ContextPropagators propagators = TraceHolder.telemetry().getPropagators();
        TextMapPropagator propagator = propagators.getTextMapPropagator();
        Context context = ctx.getOrDefault(Context.class, Context.root());
        if (null != context) {
            propagator.inject(context, source, setter::accept);
        }
        return source;
    }


    public static <T> Mono<T> writeContextTo(T source, Consumer3<T, String, String> setter) {
        if (!traceEnabled) {
            return Mono.just(source);
        }
        return Mono
                .deferContextual(ctx -> Mono.just(writeContextTo(ctx, source, setter)));
    }

    public static <D> D copyContext(Map<String, ?> source,
                                    D dest,
                                    Consumer3<D, String, String> setter) {
        if(isDisabled()){
            return dest;
        }
        return TraceHolder.writeContextTo(
                TraceHolder.readToContext(reactor.util.context.Context.empty(), source),
                dest,
                setter);
    }


}
