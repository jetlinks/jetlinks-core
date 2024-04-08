package org.jetlinks.core.trace;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor(staticName = "create")
@NoArgsConstructor
class FluxTracerBuilder<T> extends AbstractReactiveTracerBuilder<FluxTracer<T>,T> {
    private boolean fastSubscribe;

    @Override
    public FluxTracer<T> build() {
        return source ->
                new TraceFlux<>(source,
                                spanName,
                                TraceHolder.telemetry().getTracer(scopeName),
                                onNext,
                                onComplete,
                                onSubscription,
                                onError,
                                fastSubscribe,
                                defaultContext);
    }
}
