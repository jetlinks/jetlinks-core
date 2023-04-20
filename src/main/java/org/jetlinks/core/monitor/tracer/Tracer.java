package org.jetlinks.core.monitor.tracer;

import io.opentelemetry.api.trace.SpanBuilder;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.trace.ReactiveTracerBuilder;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Tracer {

    static Tracer lazy(Supplier<Tracer> lazy) {
        return new ProxyTracer(lazy);
    }


    <E> FluxTracer<E> traceFlux(String operation,
                                Consumer<ReactiveTracerBuilder<FluxTracer<E>, E>> consumer);

    default <E> FluxTracer<E> traceFlux(String operation,
                                        BiConsumer<ContextView, SpanBuilder> consumer) {
        return traceFlux(operation, (builder) -> builder.onSubscription(consumer));
    }


    <E> MonoTracer<E> traceMono(String operation,
                                Consumer<ReactiveTracerBuilder<MonoTracer<E>, E>> consumer);

    default <E> MonoTracer<E> traceMono(String operation,
                                        BiConsumer<ContextView, SpanBuilder> consumer) {
        return traceMono(operation, (builder) -> builder.onSubscription(consumer));
    }
}
