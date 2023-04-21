package org.jetlinks.core.trace;

class MonoTracerBuilder<T> extends AbstractReactiveTracerBuilder<MonoTracer<T>, T> {
    @Override
    public MonoTracer<T> build() {
        return source ->
                new TraceMono<>(source,
                                spanName,
                                TraceHolder.telemetry().getTracer(scopeName),
                                onNext,
                                onComplete,
                                onSubscription,
                                onError);
    }
}
