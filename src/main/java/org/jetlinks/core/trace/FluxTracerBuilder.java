package org.jetlinks.core.trace;

class FluxTracerBuilder<T> extends AbstractReactiveTracerBuilder<FluxTracer<T>,T> {
    @Override
    public FluxTracer<T> build() {
        return source ->
                new TraceFlux<>(source,
                                spanName,
                                TraceHolder.telemetry().getTracer(scopeName),
                                onNext,
                                onComplete,
                                onSubscription);
    }
}
