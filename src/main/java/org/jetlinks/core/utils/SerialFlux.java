package org.jetlinks.core.utils;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.publisher.SignalType;
import reactor.util.concurrent.Queues;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class SerialFlux<T> {

    @SuppressWarnings("all")
    static final AtomicIntegerFieldUpdater<SerialFlux> WIP =
            AtomicIntegerFieldUpdater.newUpdater(SerialFlux.class, "wip");

    final Queue<Pending> queue;

    volatile int wip;


    public SerialFlux() {
        this(Queues.<Pending>small().get());
    }

    public SerialFlux(Queue<Pending> queue) {
        this.queue = queue;
    }

    public int size() {
        return queue.size();
    }

    public Flux<T> join(Flux<T> join) {
        Pending pending = new Pending(join);
        if (!queue.offer(pending)) {
            return Flux.error(new IllegalStateException("pending queue is full"));
        }
        return pending;
    }

    void drain() {
        if (WIP.compareAndSet(this, 0, 1)) {
            Pending pending = queue.poll();
            if (pending != null) {
                pending.doSubscribe();
            }
        }
    }

    class Pending extends FluxOperator<T, T> {

        private final PendingSubscriber subscriber = new PendingSubscriber();

        protected Pending(Flux<? extends T> source) {
            super(source);
        }

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            subscriber.addSubscriber(actual);
            drain();
        }

        void doSubscribe() {
            source.subscribe(subscriber);
        }

        class PendingSubscriber extends BaseSubscriber<T> {
            private volatile CoreSubscriber<? super T>[] actual;

            @Override
            protected void hookFinally(@Nonnull SignalType type) {
                WIP.set(SerialFlux.this, 0);
                SerialFlux.this.drain();
            }

            @Override
            @Nonnull
            public Context currentContext() {
                Context context = super.currentContext();
                for (CoreSubscriber<? super T> coreSubscriber : actual) {
                    context = context.putAll(coreSubscriber.currentContext().readOnly());
                }
                return context;
            }

            @Override
            protected void hookOnNext(@Nonnull T value) {
                for (CoreSubscriber<? super T> coreSubscriber : actual) {
                    coreSubscriber.onNext(value);
                }
            }

            @Override
            protected void hookOnComplete() {
                for (CoreSubscriber<? super T> coreSubscriber : actual) {
                    coreSubscriber.onComplete();
                }
            }

            @Override
            protected void hookOnError(@Nonnull Throwable throwable) {
                for (CoreSubscriber<? super T> coreSubscriber : actual) {
                    coreSubscriber.onError(throwable);
                }
            }

            @Override
            protected void hookOnSubscribe(@Nonnull Subscription subscription) {
                for (CoreSubscriber<? super T> coreSubscriber : actual) {
                    coreSubscriber.onSubscribe(subscription);
                }
            }

            private synchronized void addSubscriber(CoreSubscriber<? super T> subscriber) {
                if (actual == null) {
                    actual = new CoreSubscriber[1];
                    actual[0] = subscriber;
                } else {
                    CoreSubscriber<? super T>[] newActual = new CoreSubscriber[actual.length + 1];
                    System.arraycopy(actual, 0, newActual, 0, actual.length);
                    newActual[actual.length] = subscriber;
                    actual = newActual;
                }
            }
        }


    }

}
