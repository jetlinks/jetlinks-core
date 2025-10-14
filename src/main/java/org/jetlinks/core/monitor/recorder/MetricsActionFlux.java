package org.jetlinks.core.monitor.recorder;

import jakarta.annotation.Nonnull;
import org.jetlinks.core.monitor.metrics.Metrics;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;

class MetricsActionFlux<T> extends FluxOperator<T, T> {

    private final ActionRecorder<T> action;

    protected MetricsActionFlux(ActionRecorder<T> action, Flux<? extends T> source) {
        super(source);
        this.action = action;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        this.source
            .subscribe(new MetricsActionSubscriber<>(actual, action));
    }
}
