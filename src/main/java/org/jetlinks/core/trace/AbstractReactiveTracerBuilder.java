package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import reactor.util.context.ContextView;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class AbstractReactiveTracerBuilder<T, R> implements ReactiveTracerBuilder<T, R> {
    String scopeName;
    String spanName;
    BiConsumer<Span, R> onNext;
    BiConsumer<Span, Long> onComplete;
    BiConsumer<ContextView, SpanBuilder> onSubscription;

    @Override
    public ReactiveTracerBuilder<T, R> scopeName(@Nonnull String name) {
        this.scopeName = name;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> spanName(@Nonnull String name) {
        this.spanName = name;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onNext(BiConsumer<Span, R> callback) {
        this.onNext = callback;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onComplete(BiConsumer<Span, Long> callback) {
        this.onComplete = callback;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onSubscription(BiConsumer<ContextView, SpanBuilder> callback) {
        this.onSubscription = callback;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onSubscription(Consumer<SpanBuilder> callback) {
        this.onSubscription = callback == null ? null : (contextView, spanBuilder) -> callback.accept(spanBuilder);
        return this;
    }

    @Override
    public abstract T build();
}
