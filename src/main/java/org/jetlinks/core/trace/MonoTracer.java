package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.commons.collections.MapUtils;
import reactor.core.publisher.Mono;

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
        if (TraceHolder.isDisabled(spanName)) {
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
        if (TraceHolder.isDisabled() || MapUtils.isEmpty(carrier)) {
            return unsupported();
        }
        return createWith(carrier, MapTextMapGetter.instance());
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
    static <T, R> MonoTracer<R> createWith(T source, TextMapGetter<T> getter) {
        if (TraceHolder.isDisabled()) {
            return unsupported();
        }
        return mono -> Mono
                .deferContextual(ctx -> {
                    return mono
                            .contextWrite(TraceHolder.readToContext(ctx, source, getter));
                });
    }


}
