package org.jetlinks.core.utils;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.*;
import reactor.util.concurrent.Queues;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

public class SerialFlux<T> {

    @SuppressWarnings("all")
    static final AtomicReferenceFieldUpdater<SerialFlux, Pending> WIP =
            AtomicReferenceFieldUpdater.newUpdater(SerialFlux.class, Pending.class, "wip");

    @SuppressWarnings("all")
    static final AtomicReferenceFieldUpdater<Pending.PendingSubscriber, CoreSubscriber> ACTUAL =
            AtomicReferenceFieldUpdater.newUpdater(Pending.PendingSubscriber.class, CoreSubscriber.class, "actual");

    final Queue<Pending<T>> queue;

    volatile Pending<T> wip;

    public SerialFlux() {
        this(Queues.small());
    }

    public SerialFlux(Supplier<Queue<Pending<T>>> queueSupplier) {
        this.queue = queueSupplier.get();
    }

    public int size() {
        return queue.size();
    }

    public Flux<T> join(Flux<T> join) {
        Pending<T> pending = new Pending<>(this, join);
        if (!queue.offer(pending)) {
            return Flux.error(new IllegalStateException("pending queue is full"));
        }
        return pending;
    }

    void drain() {
        if (wip != null) {
            return;
        }
        for (; ; ) {
            Pending<T> pending;
            if (WIP.compareAndSet(this, null, pending = queue.poll())) {
                if (pending != null) {
                    pending.doSubscribe();
                }
                return;
            } else {
                if (pending != null) {
                    if (!queue.offer(pending)) {
                        pending.doSubscribe();
                        return;
                    }
                }
            }
        }
    }

    static class Pending<T> extends FluxOperator<T, T> {

        private final SerialFlux<T> main;
        private final PendingSubscriber subscriber = new PendingSubscriber();

        protected Pending(SerialFlux<T> main, Flux<? extends T> source) {
            super(source);
            this.main = main;
        }

        @Override
        public Object scanUnsafe(@Nonnull Attr key) {
            if (key == Attr.TERMINATED) {
                return subscriber.isDisposed();
            }
            return super.scanUnsafe(key);
        }

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            subscriber.addSubscriber(actual);
            main.drain();
        }

        void doSubscribe() {
            if (subscriber.isCompleted()) {
                WIP.compareAndSet(main, this, null);
                main.drain();
                return;
            }
            source.subscribe(subscriber);
        }

        class PendingSubscriber extends BaseSubscriber<T> {
            @SuppressWarnings("all")
            volatile CoreSubscriber<? super T> actual = null;

            long requested = Long.MAX_VALUE;

            @Override
            protected void hookFinally(@Nonnull SignalType type) {
                complete();
            }

            private boolean isCompleted() {
                return ACTUAL.get(this) == this;
            }

            private void complete() {
                ACTUAL.set(this, this);
                if (WIP.compareAndSet(main, Pending.this, null)) {
                    main.drain();
                }
            }

            @Override
            @Nonnull
            public Context currentContext() {
                return (actual == null || isCompleted()) ? super.currentContext() : actual.currentContext();
            }

            @Override
            protected void hookOnNext(@Nonnull T value) {
                CoreSubscriber<? super T> actual = this.actual;
                if (isCompleted() || actual == null) {
                    Operators.onDiscard(value, currentContext());
                } else {
                    actual.onNext(value);
                }
            }

            @Override
            protected void hookOnComplete() {
                if (isCompleted()) {
                    return;
                }
                CoreSubscriber<? super T> actual = this.actual;
                if (actual != null) {
                    actual.onComplete();
                }
            }

            @Override
            protected void hookOnError(@Nonnull Throwable throwable) {
                if (isCompleted()) {
                    return;
                }
                CoreSubscriber<? super T> actual = this.actual;
                if (actual != null) {
                    actual.onError(throwable);
                }
            }

            @Override
            protected void hookOnSubscribe(@Nonnull Subscription subscription) {
                subscription.request(requested);
            }

            @SuppressWarnings("all")
            private synchronized void addSubscriber(CoreSubscriber<? super T> subscriber) {

                if (!ACTUAL.compareAndSet(this, null, subscriber)) {
                    Operators.error(subscriber, new IllegalStateException("SerialFlux allows only a single Subscriber"));
                    return;
                }
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        requested = n;
                    }

                    @Override
                    public void cancel() {
                        if (upstream() == null) {
                            dispose();
                            complete();
                        } else {
                            upstream().cancel();
                            complete();
                        }
                    }
                });
            }
        }


    }

}
