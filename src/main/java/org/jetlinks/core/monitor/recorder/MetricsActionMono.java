package org.jetlinks.core.monitor.recorder;

import jakarta.annotation.Nonnull;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;

class MetricsActionMono<T> extends MonoOperator<T, T> {

    private final ActionRecorder<T> action;

    protected MetricsActionMono(ActionRecorder<T> action, Mono<? extends T> source) {
        super(source);
        this.action = action;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        this.source
            .subscribe(new MetricsActionSubscriber<>(actual, action));
    }
}
