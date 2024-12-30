package org.jetlinks.core.trace;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor(staticName = "create")
@NoArgsConstructor
class MonoTracerBuilder<T> extends AbstractReactiveTracerBuilder<MonoTracer<T>, T> implements MonoTracer<T> {

    private boolean fastSubscribe;

    @Override
    public MonoTracer<T> build() {
        return this;
    }

    @Override
    public Mono<T> apply(Mono<T> source) {
        return new TraceMono<>(source,
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
