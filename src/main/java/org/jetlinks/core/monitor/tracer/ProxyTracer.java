package org.jetlinks.core.monitor.tracer;

import io.opentelemetry.api.trace.SpanBuilder;
import lombok.AllArgsConstructor;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.ReactiveSpanBuilder;
import org.jetlinks.core.trace.ReactiveTracerBuilder;
import reactor.util.context.ContextView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
    public <E> MonoTracer<E> traceMono(String operation, BiConsumer<ContextView, ReactiveSpanBuilder> consumer) {
        return lazyRef.get().traceMono(operation, consumer);
    }

    @Override
    public <E> MonoTracer<E> traceMono(String operation,
                                       Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return lazyRef.get().traceMono(operation, consumer);
    }
}
