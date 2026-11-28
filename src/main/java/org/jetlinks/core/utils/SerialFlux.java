package org.jetlinks.core.utils;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.publisher.Operators;
import reactor.util.concurrent.Queues;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Serializes subscribed work while preserving each subscriber's demand and Context. */
public class SerialFlux<T> {
    private final Queue<Pending<T>> queue;
    private final AtomicInteger draining = new AtomicInteger();
    private Pending<T> active;

    public SerialFlux() {
        this(Queues.small());
    }

    public SerialFlux(Supplier<Queue<Pending<T>>> queueSupplier) {
        this.queue = queueSupplier.get();
    }

    public synchronized int size() {
        return queue.size();
    }

    /** Accept work only when subscribed; the returned publisher allows one subscriber. */
    public Flux<T> join(Flux<T> source) {
        return new Pending<>(this, source);
    }

    private void enqueue(Pending<T> pending) {
        boolean accepted;
        synchronized (this) {
            if (pending.subscriber.done.get()) {
                return;
            }
            accepted = queue.offer(pending);
        }
        if (accepted) {
            drain();
        } else {
            pending.subscriber.onError(new IllegalStateException("pending queue is full"));
        }
    }

    private void finish(Pending<T> pending) {
        synchronized (this) {
            if (active == pending) {
                active = null;
            }
        }
        drain();
    }

    private void drain() {
        if (draining.getAndIncrement() != 0) {
            return;
        }
        int missed = 1;
        for (;;) {
            Pending<T> next = null;
            synchronized (this) {
                // The supplied queue may be SPSC; all producers and consumers use this short lock.
                while (queue.peek() != null && queue.peek().subscriber.done.get()) {
                    queue.poll();
                }
                if (active == null) {
                    active = next = queue.poll();
                }
            }
            if (next != null) {
                next.start();
            }
            // Synchronous termination schedules another turn instead of recursively subscribing.
            missed = draining.addAndGet(-missed);
            if (missed == 0) {
                return;
            }
        }
    }

    static final class Pending<T> extends FluxOperator<T, T> {
        private final SerialFlux<T> main;
        private final AtomicBoolean subscribed = new AtomicBoolean();
        private PendingSubscriber subscriber;

        private Pending(SerialFlux<T> main, Flux<T> source) {
            super(source);
            this.main = main;
        }

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            if (!subscribed.compareAndSet(false, true)) {
                Operators.error(actual, new IllegalStateException("SerialFlux allows only a single Subscriber"));
                return;
            }
            subscriber = new PendingSubscriber(actual);
            actual.onSubscribe(subscriber);
            main.enqueue(this);
        }

        private void start() {
            if (!subscriber.done.get()) {
                source.subscribe(subscriber);
            }
        }

        @Override
        public Object scanUnsafe(@Nonnull Attr key) {
            return key == Attr.TERMINATED ? subscriber != null && subscriber.done.get() : super.scanUnsafe(key);
        }

        private final class PendingSubscriber extends Operators.DeferredSubscription implements CoreSubscriber<T> {
            private final CoreSubscriber<? super T> actual;
            private final AtomicBoolean done = new AtomicBoolean();

            private PendingSubscriber(CoreSubscriber<? super T> actual) {
                this.actual = actual;
            }

            @Override
            @Nonnull
            public Context currentContext() {
                return actual.currentContext();
            }

            @Override
            public void onSubscribe(Subscription subscription) {
                set(subscription);
            }

            @Override
            public void onNext(T value) {
                if (done.get()) {
                    Operators.onDiscard(value, currentContext());
                } else {
                    actual.onNext(value);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!done.compareAndSet(false, true)) {
                    Operators.onErrorDropped(error, currentContext());
                    return;
                }
                try {
                    actual.onError(error);
                } finally {
                    main.finish(Pending.this);
                }
            }

            @Override
            public void onComplete() {
                if (done.compareAndSet(false, true)) {
                    try {
                        actual.onComplete();
                    } finally {
                        main.finish(Pending.this);
                    }
                }
            }

            @Override
            public void cancel() {
                if (done.compareAndSet(false, true)) {
                    try {
                        super.cancel();
                    } finally {
                        main.finish(Pending.this);
                    }
                }
            }
        }
    }
}
