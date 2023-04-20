package org.jetlinks.core.monitor.tracer;

import lombok.AllArgsConstructor;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.ReactiveTracerBuilder;
import org.jetlinks.core.trace.TraceHolder;

import java.util.function.Consumer;

@AllArgsConstructor
public class SimpleTracer implements Tracer {

    private final String tracePrefix;

    @Override
    public <E> FluxTracer<E> traceFlux(String operation,
                                       Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        String spanName = tracePrefix + operation;

        if (TraceHolder.isEnabled(spanName)) {
            ReactiveTracerBuilder<FluxTracer<E>, E> builder = FluxTracer.builder();
            consumer.accept(builder);
            builder.spanName(spanName);

            handleTraceBuilder(builder);

            return builder.build();
        }
        return FluxTracer.unsupported();
    }

    @Override
    public <E> MonoTracer<E> traceMono(String operation,
                                       Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        String spanName = tracePrefix + operation;

        if (TraceHolder.isEnabled(spanName)) {
            ReactiveTracerBuilder<MonoTracer<E>, E> builder = MonoTracer.builder();
            consumer.accept(builder);

            handleTraceBuilder(builder);

            builder.spanName(spanName);

            return builder.build();
        }
        return MonoTracer.unsupported();
    }

    protected void handleTraceBuilder(ReactiveTracerBuilder<?, ?> builder) {

    }
}
