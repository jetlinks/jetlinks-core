package org.jetlinks.core.trace;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;

@AllArgsConstructor
class SourceFluxTracerBuilder<T> extends AbstractReactiveTracerBuilder<Flux<T>, T> {

    private final Flux<T> source;

    @Override
    public Flux<T> build() {
        return new TraceFlux<>(source,
                               spanName,
                               TraceHolder.telemetry().getTracer(scopeName),
                               onNext,
                               onComplete,
                               onSubscription);
    }
}
