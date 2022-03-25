package org.jetlinks.core.trace;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
class SourceMonoTracerBuilder<T> extends AbstractReactiveTracerBuilder<Mono<T>, T> {

    private final Mono<T> source;

    @Override
    public Mono<T> build() {
        return new TraceFlux<>(source.flux(),
                               spanName,
                               TraceHolder.telemetry().getTracer(scopeName),
                               onNext,
                               onComplete,
                               onSubscription)
                .singleOrEmpty();
    }
}
