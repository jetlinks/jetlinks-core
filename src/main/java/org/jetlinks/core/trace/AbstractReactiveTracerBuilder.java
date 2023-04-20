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
        this.onNext = this.onNext == null ? callback : this.onNext.andThen(callback);
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onComplete(BiConsumer<Span, Long> callback) {
        this.onComplete = this.onComplete == null ? callback : this.onComplete.andThen(callback);
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onSubscription(BiConsumer<ContextView, SpanBuilder> callback) {
        this.onSubscription = this.onSubscription == null ? callback : this.onSubscription.andThen(callback);
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onSubscription(Consumer<SpanBuilder> callback) {

        return callback == null ? this : onSubscription((contextView, spanBuilder) -> callback.accept(spanBuilder));
    }

    @Override
    public abstract T build();
}
