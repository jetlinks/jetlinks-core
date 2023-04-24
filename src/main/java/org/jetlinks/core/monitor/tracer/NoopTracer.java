package org.jetlinks.core.monitor.tracer;

import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.ReactiveTracerBuilder;

import java.util.function.Consumer;

class NoopTracer implements Tracer {

    static NoopTracer INSTANCE = new NoopTracer();

    @Override
    public <E> FluxTracer<E> traceFlux(String operation, Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        return FluxTracer.unsupported();
    }

    @Override
    public <E> MonoTracer<E> traceMono(String operation, Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return MonoTracer.unsupported();
    }
}
