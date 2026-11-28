package org.jetlinks.core.utils;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.concurrent.Queues;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RejectedExecutionException;

/**
 * 将多个响应式操作串行订阅；等待区由调用方提供有限队列，不创建执行线程。
 * 操作实际订阅时才入队，取消等待者不影响正在执行的操作，释放时取消所有剩余操作。
 *
 * @since 1.3.3
 */
public class SerialFlux<T> implements Disposable {
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<SerialFlux> DRAIN =
        AtomicIntegerFieldUpdater.newUpdater(SerialFlux.class, "draining");
    private final Queue<Pending<T>> queue;
    private final int maxPending;
    private volatile int draining;
    private volatile boolean disposed;
    private volatile Pending<T> active;

    public SerialFlux() {
        this(Queues.small());
    }

    public SerialFlux(Supplier<Queue<Pending<T>>> queueSupplier) {
        queue = queueSupplier.get();
        maxPending = Integer.MAX_VALUE;
    }

    /** 有界等待区只使用数组和对象自身的短临界区，不为每个队列创建锁及条件对象。 */
    public SerialFlux(int maxPending) {
        if (maxPending <= 0) throw new IllegalArgumentException("maxPending must be positive");
        this.maxPending = maxPending;
        queue = new ArrayDeque<>(Math.min(maxPending, 16));
    }

    public synchronized int size() {
        return queue.size();
    }

    /** 返回只允许单次订阅的待执行操作；满队列以错误拒绝，不突破串行约束。 */
    public Flux<T> join(Flux<T> source) {
        return new Pending<>(this, source);
    }

    /** Mono 的基数由源保证，复用相同队列而无需再次检查单值。 */
    public Mono<T> join(Mono<T> source) {
        return Mono.fromDirect(join(source.flux()));
    }

    private void enqueue(Pending<T> pending) {
        boolean accepted;
        synchronized (this) {
            if (pending.subscriber.cancelled()) return;
            accepted = !disposed && queue.size() < maxPending && queue.offer(pending);
        }
        if (!accepted) pending.subscriber.onError(disposed
            ? new CancellationException("SerialFlux is disposed")
            : new RejectedExecutionException("pending queue is full"));
        else drain();
    }

    private void finished(Pending<T> pending) {
        synchronized (this) {
            if (active == pending) active = null;
            else {
                // 调用方的队列可能不支持 remove；只用 poll/offer 保留剩余操作的顺序。
                for (int remaining = queue.size(); remaining > 0; remaining--) {
                    Pending<T> queued = queue.poll();
                    if (queued != null && queued != pending) queue.offer(queued);
                }
            }
        }
        drain();
    }

    private void drain() {
        // 同步完成及下游重入也经此循环，避免递归订阅和竞争时并行执行。
        if (DRAIN.getAndIncrement(this) != 0) return;
        int missed = 1;
        do {
            Pending<T> next = null;
            synchronized (this) {
                if (active == null && !disposed) active = next = queue.poll();
            }
            if (next != null) {
                if (next.subscriber.cancelled()) finished(next);
                else next.start();
            }
            missed = DRAIN.addAndGet(this, -missed);
        } while (missed != 0);
    }

    @Override
    public void dispose() {
        Pending<T> running;
        synchronized (this) {
            disposed = true;
            running = active;
        }
        if (running != null) running.subscriber.abort();
        for (;;) {
            Pending<T> pending;
            synchronized (this) { pending = queue.poll(); }
            if (pending == null) break;
            pending.subscriber.abort();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    static final class Pending<T> extends FluxOperator<T, T> {
        private final SerialFlux<T> main;
        private final PendingSubscriber subscriber = new PendingSubscriber();
        @SuppressWarnings("rawtypes")
        private static final AtomicReferenceFieldUpdater<Pending, CoreSubscriber> ACTUAL =
            AtomicReferenceFieldUpdater.newUpdater(Pending.class, CoreSubscriber.class, "actual");
        @SuppressWarnings("rawtypes")
        private static final AtomicIntegerFieldUpdater<Pending> TERMINATED =
            AtomicIntegerFieldUpdater.newUpdater(Pending.class, "terminated");
        private volatile CoreSubscriber<? super T> actual;
        private volatile int terminated;

        Pending(SerialFlux<T> main, Flux<? extends T> source) {
            super(source);
            this.main = main;
        }

        void start() {
            source.subscribe(subscriber);
        }

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            if (!ACTUAL.compareAndSet(this, null, actual)) {
                Operators.error(actual, new IllegalStateException("SerialFlux allows only a single Subscriber"));
                return;
            }
            actual.onSubscribe(subscriber);
            if (!subscriber.cancelled()) main.enqueue(this);
        }

        final class PendingSubscriber extends Operators.DeferredSubscription implements CoreSubscriber<T> {
            private boolean cancelled() { return isCancelled(); }

            @Override public Context currentContext() { return actual.currentContext(); }
            @Override public void onSubscribe(Subscription subscription) { set(subscription); }
            @Override public void onNext(T value) {
                if (isCancelled() || terminated != 0) Operators.onDiscard(value, currentContext());
                else actual.onNext(value);
            }
            @Override public void onError(Throwable error) {
                if (isCancelled() || !TERMINATED.compareAndSet(Pending.this, 0, 1)) {
                    if (!(error instanceof CancellationException)) Operators.onErrorDropped(error, currentContext());
                    return;
                }
                try { actual.onError(error); }
                finally { main.finished(Pending.this); }
            }
            @Override public void onComplete() {
                if (isCancelled() || !TERMINATED.compareAndSet(Pending.this, 0, 1)) return;
                try { actual.onComplete(); }
                finally { main.finished(Pending.this); }
            }
            @Override public void cancel() {
                super.cancel();
                main.finished(Pending.this);
            }
            private void abort() {
                try { onError(new CancellationException("SerialFlux is disposed")); }
                finally { super.cancel(); }
            }
        }
    }
}
