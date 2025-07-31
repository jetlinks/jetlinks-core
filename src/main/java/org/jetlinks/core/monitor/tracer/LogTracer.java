package org.jetlinks.core.monitor.tracer;

import lombok.AllArgsConstructor;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.ReactiveTracerBuilder;

import java.util.function.Consumer;

@AllArgsConstructor
public class LogTracer implements Tracer{

    private final String logger;

    @Override
    public <E> FluxTracer<E> traceFlux(String operation,
                                       Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        return flux->flux.log(logger+"."+operation);
    }

    @Override
    public <E> MonoTracer<E> traceMono(String operation,
                                       Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return mono->mono.log(logger+"."+operation);
    }
}
