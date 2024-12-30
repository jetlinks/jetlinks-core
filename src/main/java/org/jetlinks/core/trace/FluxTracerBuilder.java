package org.jetlinks.core.trace;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;

@AllArgsConstructor(staticName = "create")
@NoArgsConstructor
class FluxTracerBuilder<T> extends AbstractReactiveTracerBuilder<FluxTracer<T>, T> implements FluxTracer<T> {
    private boolean fastSubscribe;

    @Override
    public FluxTracer<T> build() {
        return this;
    }

    @Override
    public Flux<T> apply(Flux<T> source) {
        return new TraceFlux<>(source,
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
