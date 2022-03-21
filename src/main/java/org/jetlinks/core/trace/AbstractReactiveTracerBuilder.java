package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import reactor.util.context.ContextView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract class AbstractReactiveTracerBuilder<T, R> implements ReactiveTracerBuilder<T, R> {
    String scopeName;
    String spanName;
    BiConsumer<Span, R> onNext;
    BiConsumer<Span, Boolean> onComplete;
    BiConsumer<ContextView,SpanBuilder> onSubscription;

    @Override
    public ReactiveTracerBuilder<T, R> scopeName(String name) {
        this.scopeName = name;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> spanName(String name) {
        this.spanName = name;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onNext(BiConsumer<Span, R> onNext) {
        this.onNext = onNext;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onComplete(BiConsumer<Span, Boolean> onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onSubscription(BiConsumer<ContextView,SpanBuilder> onSubscription) {
        this.onSubscription = onSubscription;
        return this;
    }

    @Override
    public ReactiveTracerBuilder<T, R> onSubscription(Consumer<SpanBuilder> consumer) {
        this.onSubscription = consumer==null?null:(contextView, spanBuilder) -> consumer.accept(spanBuilder);
        return this;
    }

    @Override
    public abstract T build();
}
