package org.jetlinks.core.monitor.recorder;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.util.context.Context;

@AllArgsConstructor
class MetricsActionSubscriber<T> extends BaseSubscriber<T> {

    final CoreSubscriber<? super T> actual;

    final ActionRecorder<? super T> action;

    @Override
    protected void hookOnSubscribe(@Nonnull Subscription subscription) {
        action.start(currentContext());
        actual.onSubscribe(this);
    }

    @Override
    @Nonnull
    public Context currentContext() {
        return actual.currentContext();
    }

    @Override
    protected void hookOnCancel() {
        action.cancel();
    }

    @Override
    protected void hookOnError(@Nonnull Throwable throwable) {
        action.error(throwable);
        actual.onError(throwable);
    }

    @Override
    protected void hookOnNext(@Nonnull T value) {
        action.value(value);
        actual.onNext(value);
    }

    @Override
    protected void hookOnComplete() {
        action.complete();
        actual.onComplete();
    }

}
