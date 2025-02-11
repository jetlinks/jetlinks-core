package org.jetlinks.core.monitor.tracer;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import lombok.AllArgsConstructor;
import org.jetlinks.core.trace.*;
import reactor.util.context.ContextView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
class ProxyTracer implements Tracer {

    private final Supplier<Tracer> lazyRef;

    @Override
    public <E> FluxTracer<E> traceFlux(String operation,
                                       Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        return lazyRef.get().traceFlux(operation, consumer);
    }

    @Override
    public <E> FluxTracer<E> traceFlux(String operation, BiConsumer<ContextView, ReactiveSpanBuilder> consumer) {
        return lazyRef.get().traceFlux(operation, consumer);
    }

    @Override
    public <E> FluxTracer<E> traceFlux(CharSequence operation) {
        return lazyRef.get().traceFlux(operation);
    }

    @Override
    public <E> FluxTracer<E> traceFlux(CharSequence operation, Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        return lazyRef.get().traceFlux(operation, consumer);
    }

    @Override
    public <E> FluxTracer<E> traceFlux(CharSequence operation, BiConsumer<ContextView, ReactiveSpanBuilder> consumer) {
        return lazyRef.get().traceFlux(operation, consumer);
    }

    @Override
    public <E> MonoTracer<E> traceMono(CharSequence operation) {
        return lazyRef.get().traceMono(operation);
    }

    @Override
    public <E> MonoTracer<E> traceMono(CharSequence operation, Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return lazyRef.get().traceMono(operation, consumer);
    }

    @Override
    public <E> FluxTracer<E> traceFlux(String operation) {
        return lazyRef.get().traceFlux(operation);
    }

    @Override
    public <E> MonoTracer<E> traceMono(String operation) {
        return lazyRef.get().traceMono(operation);
    }

    @Override
    public <E> MonoTracer<E> traceMono(String operation, BiConsumer<ContextView, ReactiveSpanBuilder> consumer) {
        return lazyRef.get().traceMono(operation, consumer);
    }

    @Override
    public <E> MonoTracer<E> traceMono(String operation,
                                       Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return lazyRef.get().traceMono(operation, consumer);
    }

    @Override
    public <E> E traceBlocking(CharSequence operation, ContextView ctx, Function<ReactiveSpan, E> task) {
        return lazyRef.get().traceBlocking(operation, ctx, task);
    }

    @Override
    public <E> E traceBlocking(CharSequence operation, Function<ReactiveSpan, E> task) {
        return lazyRef.get().traceBlocking(operation, task);
    }
}
