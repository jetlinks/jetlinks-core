package org.jetlinks.core.utils;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;

/**
 * 按时间窗口去重
 *
 * @param <T> 泛型
 * @see FluxUtils#distinct(Function, Duration)
 */
public class DistinctDurationFlux<T> extends FluxOperator<T, T> {

    private final Function<T, ?> keySelector;
    private final Duration duration;

    protected DistinctDurationFlux(Flux<? extends T> source,
                                   Function<T, ?> keySelector,
                                   Duration duration) {
        super(source);
        this.keySelector = keySelector;
        this.duration = duration;
    }

    public static <T> Flux<T> create(Flux<? extends T> source,
                                                     Function<T, ?> keySelector,
                                                     Duration duration) {
        return new DistinctDurationFlux<>(source, keySelector, duration)
            //.onBackpressureBuffer()
            ;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
        source.subscribe(new DistinctDurationSubscriber<>(actual, keySelector, duration.toMillis()));
    }

    static class DistinctDurationSubscriber<T> extends ConcurrentHashMap<Object, Long>
        implements CoreSubscriber<T>, Subscription, Runnable, Scannable {
        private final CoreSubscriber<? super T> actual;
        private final Function<T, ?> keySelector;
        private final long expires;
        private final Disposable disposable;

        private volatile Subscription subscription;
        @SuppressWarnings("all")
        static final AtomicReferenceFieldUpdater<DistinctDurationSubscriber, Subscription> S =
            AtomicReferenceFieldUpdater.newUpdater(DistinctDurationSubscriber.class, Subscription.class, "subscription");

        DistinctDurationSubscriber(CoreSubscriber<? super T> actual,
                                   Function<T, ?> keySelector,
                                   long expires) {
            this.actual = actual;
            this.keySelector = keySelector;
            this.expires = expires;
            int interval = (int) (expires * 1.1D);
            //定时清理内存
            this.disposable = Schedulers
                .parallel()
                .schedulePeriodically(this, interval, interval, TimeUnit.MILLISECONDS);
        }


        @Override
        @Nonnull
        public Context currentContext() {
            return actual.currentContext();
        }

        @Override
        public void run() {
            cleanup();
        }

        private void cleanup() {
            long now = System.currentTimeMillis();
            forEach((key, last) -> {
                if (now - last > expires) {
                    remove(key);
                }
            });
        }

        @Override
        public void request(long n) {
            if (Operators.validate(n)) {
                Subscription s = this.subscription;
                if (s != null) {
                    s.request(n);
                }
            }
        }

        @Override
        public void cancel() {
            if (Operators.terminate(S, this)) {
                complete();
            }
        }

        @Override
        public void onSubscribe(@Nonnull Subscription s) {
            if (Operators.setOnce(S, this, s)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            try {
                long now = System.currentTimeMillis();
                Object key = keySelector.apply(t);
                if (key == null) {
                    actual.onNext(t);
                    return;
                }
                Long last = putIfAbsent(key, now);
                if (last == null || now - last > expires) {
                    actual.onNext(t);
                } else {
                    Operators.onDiscard(t, actual.currentContext());
                    request(1);
                }
            } catch (Throwable e) {
                onError(e);
            }

        }

        @Override
        public void onError(Throwable t) {
            try {
                if (S.getAndSet(this, Operators.cancelledSubscription()) == Operators
                    .cancelledSubscription()) {
                    //already cancelled concurrently
                    Operators.onErrorDropped(t, currentContext());
                    return;
                }
                actual.onError(t);
            } catch (Throwable e) {
                Operators.onErrorDropped(Exceptions.addSuppressed(t, e), currentContext());
            } finally {
                complete();
            }

        }

        @Override
        public void onComplete() {
            if (S.getAndSet(this, Operators.cancelledSubscription()) != Operators
                .cancelledSubscription()) {
                try {
                    actual.onComplete();
                } catch (Throwable e) {
                    Operators.onErrorDropped(e, currentContext());
                } finally {
                    complete();
                }
            }

        }

        private void complete() {
            this.clear();
            disposable.dispose();
        }

        @Override
        public Object scanUnsafe(@Nonnull Attr key) {
            if (key == Attr.RUN_STYLE) return Attr.RunStyle.SYNC;
            if (key == Attr.ACTUAL) return actual;
            return null;
        }
    }
}
