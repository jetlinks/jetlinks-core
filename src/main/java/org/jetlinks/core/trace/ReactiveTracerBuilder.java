package org.jetlinks.core.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import reactor.util.context.ContextView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ReactiveTracerBuilder<T,R> {

    ReactiveTracerBuilder<T,R> scopeName(String name);

    ReactiveTracerBuilder<T,R> spanName(String name);

    ReactiveTracerBuilder<T,R> onNext(BiConsumer<Span,R> consumer);

    ReactiveTracerBuilder<T,R> onComplete(BiConsumer<Span,Boolean> consumer);

    ReactiveTracerBuilder<T,R> onSubscription(BiConsumer<ContextView,SpanBuilder> consumer);

    ReactiveTracerBuilder<T,R> onSubscription(Consumer<SpanBuilder> consumer);

    T build();
}
