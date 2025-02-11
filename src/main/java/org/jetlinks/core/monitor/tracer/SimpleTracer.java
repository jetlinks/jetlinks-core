package org.jetlinks.core.monitor.tracer;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.trace.*;
import reactor.util.context.ContextView;

import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleTracer implements Tracer {

    private final SeparatedCharSequence tracePrefix;

    public SimpleTracer(String tracePrefix) {
        this(SharedPathString.of(tracePrefix));
    }
    public SimpleTracer(SeparatedCharSequence tracePrefix) {
        this.tracePrefix = tracePrefix;
    }

    @Override
    public <E> FluxTracer<E> traceFlux(CharSequence operation,
                                       Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        SeparatedCharSequence spanName = tracePrefix.append(operation);

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
    public <E> FluxTracer<E> traceFlux(String operation,
                                       Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer) {
        return traceFlux((CharSequence) operation, consumer);
    }

    public <E> MonoTracer<E> traceMono(String operation,
                                       Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        return traceMono((CharSequence) operation, consumer);
    }

    @Override
    public <E> MonoTracer<E> traceMono(CharSequence operation,
                                       Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer) {
        SeparatedCharSequence spanName = tracePrefix.append(operation);

        if (TraceHolder.isEnabled(spanName)) {
            ReactiveTracerBuilder<MonoTracer<E>, E> builder = MonoTracer.builder();
            consumer.accept(builder);

            handleTraceBuilder(builder);

            builder.spanName(spanName);

            return builder.build();
        }
        return MonoTracer.unsupported();
    }

    @Override
    public <E> E traceBlocking(CharSequence operation, Function<ReactiveSpan, E> task) {
        SeparatedCharSequence spanName = tracePrefix.append(operation);

        return TraceHolder.traceBlocking(spanName, task);
    }

    @Override
    public <E> E traceBlocking(CharSequence operation, ContextView ctx, Function<ReactiveSpan, E> task) {
        SeparatedCharSequence spanName = tracePrefix.append(operation);
        return TraceHolder.traceBlocking(
            ctx.getOrDefault(Context.class, Context.current()),
            spanName,
            task);
    }

    protected void handleTraceBuilder(ReactiveTracerBuilder<?, ?> builder) {

    }
}
