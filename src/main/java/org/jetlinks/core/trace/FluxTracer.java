package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FluxTracer<T> extends Function<Flux<T>, Flux<T>> {

    static <T> FluxTracer<T> unsupported() {
        return source -> source;
    }

    static <T> FluxTracer<T> create(String spanName) {
        return create(TraceHolder.appName(), spanName);
    }

    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName) {
        return create(scopeName, spanName, null, null);
    }

    static <T> FluxTracer<T> create(String spanName,
                                    BiConsumer<Span, T> onNext) {
        return create(TraceHolder.appName(), spanName, onNext, null);
    }

    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<Span, T> onNext) {
        return create(scopeName, spanName, onNext, null);
    }

    static <T> FluxTracer<T> create(String spanName,
                                    Consumer<SpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, null, builderConsumer);
    }

    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName,
                                    Consumer<SpanBuilder> builderConsumer) {
        return create(scopeName, spanName, null, builderConsumer);
    }


    static <T> FluxTracer<T> create(String spanName,
                                    BiConsumer<Span, T> onNext,
                                    Consumer<SpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, onNext, builderConsumer);
    }

    static <T> FluxTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<Span, T> onNext,
                                    Consumer<SpanBuilder> builderConsumer) {
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        return source ->
                new TraceFlux<>(source,
                                spanName,
                                TraceHolder.telemetry().getTracer(scopeName),
                                onNext,
                                builderConsumer
                );
    }


    @SuppressWarnings("all")
    static <R> FluxTracer<R> createWith(Map<String, ?> carrier) {
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        return createWith(carrier, new TextMapGetter<Map<String, ?>>() {
            @Override
            public Iterable<String> keys(Map<String, ?> carrier) {
                return carrier.keySet();
            }

            @Nullable
            @Override
            public String get(@Nullable Map<String, ?> carrier, String key) {
                return (String) carrier.get(key);
            }
        });
    }

    @SuppressWarnings("all")
    static <T, R> FluxTracer<R> createWith(T source, TextMapGetter<T> setter) {
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        ContextPropagators propagators = TraceHolder.telemetry().getPropagators();
        TextMapPropagator propagator = propagators.getTextMapPropagator();

        return flux -> Flux
                .deferContextual(ctx -> {
                    Context context = ctx.getOrDefault(Context.class, Context.root());
                    if (null != context) {
                        context = propagator.extract(context, source, setter);
                        return flux
                                .contextWrite(reactor.util.context.Context
                                                      .of(Context.class, context));
                    }
                    return flux;
                });
    }


}
