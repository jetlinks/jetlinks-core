package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface MonoTracer<T> extends Function<Mono<T>, Mono<T>> {

    static <T> MonoTracer<T> unsupported() {
        return source -> source;
    }

    static <T> MonoTracer<T> create(String spanName) {
        return create(TraceHolder.appName(), spanName);
    }

    static <T> MonoTracer<T> create(String scopeName,
                                    String spanName) {
        return create(scopeName, spanName, null, null);
    }

    static <T> MonoTracer<T> create(String spanName,
                                    BiConsumer<Span, T> onNext) {
        return create(TraceHolder.appName(), spanName, onNext, null);
    }

    static <T> MonoTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<Span, T> onNext) {
        return create(scopeName, spanName, onNext, null);
    }

    static <T> MonoTracer<T> create(String spanName,
                                    Consumer<SpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, null, builderConsumer);
    }

    static <T> MonoTracer<T> create(String scopeName,
                                    String spanName,
                                    Consumer<SpanBuilder> builderConsumer) {
        return create(scopeName, spanName, null, builderConsumer);
    }


    static <T> MonoTracer<T> create(String spanName,
                                    BiConsumer<Span, T> onNext,
                                    Consumer<SpanBuilder> builderConsumer) {
        return create(TraceHolder.appName(), spanName, onNext, builderConsumer);
    }

    static <T> MonoTracer<T> create(String scopeName,
                                    String spanName,
                                    BiConsumer<Span, T> onNext,
                                    Consumer<SpanBuilder> builderConsumer) {
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        return source ->
                new TraceFlux<>(source.flux(),
                                spanName,
                                TraceHolder.telemetry().getTracer(scopeName),
                                onNext,
                                builderConsumer)
                        .singleOrEmpty();
    }


    @SuppressWarnings("all")
    static <R> MonoTracer<R> createWith(Map<String, ?> carrier) {
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

    /**
     * 基于一个信号构造追踪器，如: 通过http header来获取上下文并进行追踪
     * <pre>{@code
     *
     *     chain
     *     .filter(exchange)
     *     .as(traceWith(headers,HttpHeaders::getFirst))
     * }
     * </pre>
     *
     * @param carrier 信号
     * @param <R>
     * @return 追踪器
     */
    @SuppressWarnings("all")
    static <T, R> MonoTracer<R> createWith(T source, TextMapGetter<T> setter) {
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        ContextPropagators propagators = TraceHolder.telemetry().getPropagators();
        TextMapPropagator propagator = propagators.getTextMapPropagator();

        return mono -> Mono
                .deferContextual(ctx -> {
                    Context context = ctx.getOrDefault(Context.class, Context.root());
                    if (null != context) {
                        context = propagator.extract(context, source, setter);
                        return mono
                                .contextWrite(reactor.util.context.Context
                                                      .of(Context.class, context));
                    }
                    return mono;
                });
    }


}
