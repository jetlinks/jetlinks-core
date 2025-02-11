package org.jetlinks.core.monitor.tracer;

import io.opentelemetry.api.trace.Span;
import org.jetlinks.core.trace.*;
import reactor.util.context.ContextView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class NoopTracer implements Tracer {

    static NoopTracer INSTANCE = new NoopTracer();

    @Override
    public <E> MonoTracer<E> traceMono(CharSequence operation, Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return MonoTracer.unsupported();
    }

    @Override
    public <E> MonoTracer<E> traceMono(CharSequence operation) {
        return MonoTracer.unsupported();
    }

    @Override
    public <E> FluxTracer<E> traceFlux(CharSequence operation, BiConsumer<ContextView, ReactiveSpanBuilder> consumer) {
        return FluxTracer.unsupported();
    }

    @Override
    public <E> FluxTracer<E> traceFlux(CharSequence operation, Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        return FluxTracer.unsupported();
    }

    @Override
    public <E> FluxTracer<E> traceFlux(CharSequence operation) {
        return FluxTracer.unsupported();
    }

    @Override
    public <E> FluxTracer<E> traceFlux(String operation, Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        return FluxTracer.unsupported();
    }

    @Override
    public <E> MonoTracer<E> traceMono(String operation, Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return MonoTracer.unsupported();
    }

    @Override
    public <E> E traceBlocking(CharSequence operation, ContextView ctx, Function<ReactiveSpan, E> task) {
        return task.apply(ReactiveSpan.noop());
    }

    @Override
    public <E> E traceBlocking(CharSequence operation, Function<ReactiveSpan, E> task) {
        return task.apply(ReactiveSpan.noop());
    }
}
