package org.jetlinks.core.trace;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor(staticName = "create")
@NoArgsConstructor
class MonoTracerBuilder<T> extends AbstractReactiveTracerBuilder<MonoTracer<T>, T> {

    private boolean fastSubscribe;
    @Override
    public MonoTracer<T> build() {
        return source ->
                new TraceMono<>(source,
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
