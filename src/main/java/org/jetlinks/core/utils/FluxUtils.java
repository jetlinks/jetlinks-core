package org.jetlinks.core.utils;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.*;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.*;

public class FluxUtils {


    /**
     * 构造有效期内去重的Flux
     *
     * <pre>
     *    flux.as(ReactorUtils.distinct(MyData::getId,Duration.ofSeconds(30)))
     * </pre>
     *
     * @param keySelector 去重的key
     * @param duration    有效期
     * @param <T>         泛型
     * @return 去重构造器
     */
    public static <T> Function<Flux<T>, Flux<T>> distinct(Function<T, ?> keySelector, Duration duration) {
        return flux -> DistinctDurationFlux.create(flux, keySelector, duration);
    }

    /**
     * @deprecated {@link  Flux#mapNotNull(Function)}
     */
    public static <S, T> Function<Flux<S>, Flux<T>> safeMap(Function<S, T> mapper) {
        return source -> source.mapNotNull(mapper);
    }


    public static <T> Flux<List<T>> bufferRate(Flux<T> flux,
                                               int rate,
                                               Duration maxTimeout) {
        return bufferRate(flux, rate, 100, maxTimeout);
    }

    public static <T> Flux<List<T>> bufferRate(Flux<T> flux,
                                               int rate,
                                               int maxSize,
                                               Duration maxTimeout) {
        return Flux.create(sink -> {
            BufferRateSubscriber<T> subscriber = new BufferRateSubscriber<>(sink, maxSize, rate, maxTimeout, (e, arr) -> arr
                .size() >= maxSize);

            flux.elapsed().subscribe(subscriber);

            sink.onDispose(subscriber);
        });

    }

    public static <T> Flux<List<T>> bufferRate(Flux<T> flux,
                                               int rate,
                                               int maxSize,
                                               Duration maxTimeout,
                                               BiPredicate<T, List<T>> flushCondition) {
        return Flux.create(sink -> {
            BufferRateSubscriber<T> subscriber = new BufferRateSubscriber<>(sink, maxSize, rate, maxTimeout, (e, arr) -> flushCondition
                .test(e, arr) || arr.size() >= maxSize);

            flux.elapsed().subscribe(subscriber);

            sink.onDispose(subscriber);
        });

    }

    static class BufferRateSubscriber<T> extends BaseSubscriber<Tuple2<Long, T>> {
        int bufferSize;
        int rate;

        volatile List<T> bufferArray;
        FluxSink<List<T>> sink;

        Duration timeout;
        Scheduler timer = Schedulers.parallel();
        Disposable timerDispose;

        private final BiPredicate<T, List<T>> flushCondition;

        BufferRateSubscriber(FluxSink<List<T>> sink,
                             int bufferSize,
                             int rate,
                             Duration timeout,
                             BiPredicate<T, List<T>> flushCondition) {
            this.sink = sink;
            this.bufferSize = bufferSize;
            this.rate = rate;
            this.timeout = timeout;
            this.flushCondition = flushCondition;
            newBuffer();
        }

        protected List<T> newBuffer() {
            List<T> buffer = bufferArray;
            bufferArray = new ArrayList<>(bufferSize);
            return buffer;
        }

        @Override
        protected void hookFinally(@Nonnull SignalType type) {
            doFlush();
        }

        void doFlush() {
            if (!bufferArray.isEmpty()) {
                sink.next(newBuffer());
            }
            request(bufferSize);
            if (timerDispose != null && !timerDispose.isDisposed()) {
                timerDispose.dispose();
            }
        }

        @Override
        protected void hookOnSubscribe(@Nonnull Subscription subscription) {
            request(bufferSize);
        }

        @Override
        protected void hookOnNext(Tuple2<Long, T> value) {
            bufferArray.add(value.getT2());
            if (value.getT1() > rate) {
                doFlush();
            } else {
                if (flushCondition.test(value.getT2(), bufferArray)) {
                    doFlush();
                } else {
                    if (timerDispose == null || timerDispose.isDisposed()) {
                        timerDispose = timer.schedule(this::doFlush, timeout.toMillis(), TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    /**
     * 根据背压自动合并数据流
     * <p>
     * 当发生背压时，数据会被缓冲到队列中。
     * 当下游请求数据时，会将缓冲的数据合并后发送。
     * 使用 bufferPredicate 来判断是否还需要继续缓冲数据。
     *
     * @param source            源数据流
     * @param containerSupplier 容器供应商，用于累积合并数据
     * @param merger            合并函数，将元素合并到容器中。注意：此函数不应该修改原容器，应该返回新容器。
     *                          如果修改了原容器，可能导致不正确的结果。
     * @param bufferPredicate   缓冲条件，返回true表示可以继续缓冲，false表示应该发送当前容器
     * @param mapper            容器转换函数，将容器转换为目标类型
     * @param onDrop            元素丢弃时的回调，用于释放资源（如ByteBuf的引用计数）
     * @param <S>               源元素类型
     * @param <C>               容器类型
     * @param <T>               转换后的目标类型
     * @return 合并并转换后的数据流
     */
    public static <S, C, T> Flux<T> mergeOnBackpressure(Flux<S> source,
                                                        Supplier<C> containerSupplier,
                                                        BiFunction<C, S, C> merger,
                                                        Predicate<C> bufferPredicate,
                                                        Function<C, T> mapper,
                                                        Consumer<S> onDrop) {
        return new MergeOnBackpressureOperator<>(source, containerSupplier, merger, bufferPredicate, mapper, onDrop);
    }

    /**
     * 根据背压自动合并的FluxOperator
     */
    static class MergeOnBackpressureOperator<S, C, T> extends FluxOperator<S, T> {
        private final Supplier<C> containerSupplier;
        private final BiFunction<C, S, C> merger;
        private final Predicate<C> bufferPredicate;
        private final Function<C, T> mapper;
        private final Consumer<S> onDrop;

        protected MergeOnBackpressureOperator(Flux<? extends S> source,
                                              Supplier<C> containerSupplier,
                                              BiFunction<C, S, C> merger,
                                              Predicate<C> bufferPredicate,
                                              Function<C, T> mapper,
                                              Consumer<S> onDrop) {
            super(source);
            this.containerSupplier = containerSupplier;
            this.merger = merger;
            this.bufferPredicate = bufferPredicate;
            this.mapper = mapper;
            this.onDrop = onDrop;
        }

        @Override
        public void subscribe(@Nonnull CoreSubscriber<? super T> actual) {
            source.subscribe(
                new MergeOnBackpressureSubscriber<>(containerSupplier, merger, bufferPredicate, mapper, onDrop, actual)
            );
        }
    }

    /**
     * 根据背压自动合并的Subscriber
     */
    static class MergeOnBackpressureSubscriber<S, C, T> implements CoreSubscriber<S>, Subscription {
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<MergeOnBackpressureSubscriber> WIP = AtomicIntegerFieldUpdater
            .newUpdater(MergeOnBackpressureSubscriber.class, "wip");
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<MergeOnBackpressureSubscriber> REQUESTED = AtomicLongFieldUpdater
            .newUpdater(MergeOnBackpressureSubscriber.class, "requested");
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<MergeOnBackpressureSubscriber> REMAINDER = AtomicLongFieldUpdater
            .newUpdater(MergeOnBackpressureSubscriber.class, "remainder");

        private final Supplier<C> containerSupplier;
        private final BiFunction<C, S, C> merger;
        private final Predicate<C> bufferPredicate;
        private final Function<C, T> mapper;
        private final Consumer<S> onDrop;
        private final CoreSubscriber<? super T> actual;

        private volatile long requested;
        private volatile long remainder; // 剩余请求数量（已请求但未消费）
        private volatile int wip;

        private volatile boolean done;
        private volatile Throwable error;
        private volatile boolean cancelled;

        private volatile Subscription s;

        private final Queue<S> queue = Queues.<S>unboundedMultiproducer().get();

        private volatile C currentContainer; // 当前正在累积的容器

        MergeOnBackpressureSubscriber(Supplier<C> containerSupplier,
                                      BiFunction<C, S, C> merger,
                                      Predicate<C> bufferPredicate,
                                      Function<C, T> mapper,
                                      Consumer<S> onDrop,
                                      CoreSubscriber<? super T> actual) {
            this.containerSupplier = containerSupplier;
            this.merger = merger;
            this.bufferPredicate = bufferPredicate;
            this.mapper = mapper;
            this.onDrop = onDrop;
            this.actual = actual;
        }

        @Override
        @Nonnull
        public Context currentContext() {
            return actual.currentContext();
        }

        @Override
        public void onSubscribe(@Nonnull Subscription subscription) {
            if (this.s != null) {
                subscription.cancel();
                return;
            }
            this.s = subscription;
            actual.onSubscribe(this);
            // 不主动请求初始数据，等待下游请求（符合Reactive Streams规范）
        }

        @Override
        public void onNext(@Nonnull S element) {
            // 收到上游数据，减少 remainder（无论是否处理）
            Operators.produced(REMAINDER, this, 1);
            if (done || cancelled) {
                releaseElement(element);
                return;
            }
            queue.offer(element);
            drain();
        }

        @Override
        public void onError(@Nonnull Throwable t) {
            if (done) return;
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            if (done) return;
            done = true;
            drain();
        }

        @Override
        public void request(long n) {
            if (Operators.validate(n)) {
                Operators.addCap(REQUESTED, this, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            s.cancel();
            drain();
        }

        void drain() {
            if (WIP.getAndIncrement(this) != 0) {
                return;
            }
            int missed = 1;
            final CoreSubscriber<? super T> a = this.actual;

            for (; ; ) {
                if (cancelled) {
                    cleanup();
                    return;
                }

                long r = requested;
                long e = 0L;

                // 当下游有请求时，合并并发送数据
                while (e != r) {
                    if (cancelled) {
                        cleanup();
                        return;
                    }

                    boolean d = done;
                    boolean empty = queue.isEmpty();

                    // 在循环开始时判断是否有背压：requested == 0 或队列不为空
                    // 这个判断需要在处理队列之前进行，因为处理过程中队列状态会变化
                    boolean hasBackpressure = (r == 0) || !empty;

                    if (d && empty) {
                        terminate(a);
                        return;
                    }

                    if (empty) {
                        // 如果队列为空且上游已完成，发送当前容器（如果有）
                        if (done && currentContainer != null) {
                            T mapped = mapper.apply(currentContainer);
                            a.onNext(mapped);
                            currentContainer = null; // 重置容器
                            e++;
                            r = requested;
                        }
                        break;
                    }

                    // 获取或创建当前容器
                    C container = currentContainer != null ? currentContainer : containerSupplier.get();
                    boolean hasMerged = false;
                    boolean shouldEmit = false;

                    // 合并数据，直到bufferPredicate返回false或队列为空
                    // 重要：为了避免资源泄漏（如 ByteBuf 引用计数问题），采用"先poll再merge"的策略
                    // 这确保了元素一旦从队列中取出，就必须被处理，避免 merger 消费资源后元素还留在队列中
                    while (!queue.isEmpty()) {
                        S element = queue.poll();
                        if (element == null) {
                            break;
                        }

                        // 合并元素到容器中
                        C newContainer = merger.apply(container, element);

                        // 使用bufferPredicate判断合并后的容器是否还可以继续缓冲
                        boolean canBuffer = bufferPredicate.test(newContainer);

                        // 更新容器
                        container = newContainer;
                        hasMerged = true;

                        if (!canBuffer) {
                            // 不能再缓冲了，标记应该发送
                            shouldEmit = true;
                            // 停止合并
                            break;
                        }
                    }

                    // 更新当前容器
                    if (hasMerged) {
                        currentContainer = container;
                    }

                    // 在处理完队列后，重新检查是否有背压
                    // 因为处理过程中队列可能已经变为空
                    boolean queueEmptyAfterProcessing = queue.isEmpty();
                    boolean hasBackpressureAfterProcessing = (r == 0) || !queueEmptyAfterProcessing;

                    // 发送条件判断：
                    // 1. bufferPredicate返回false（不能再缓冲了） -> 必须发送
                    // 2. 没有背压且合并了元素 -> 立即发送（避免延迟）
                    // 3. 上游已完成且合并了元素 -> 立即发送（确保流能结束）
                    // 4. 有背压且上游未完成且可以继续缓冲 -> 保存容器状态，等待更多元素
                    if (hasMerged) {
                        boolean shouldSend = shouldEmit  // bufferPredicate返回false
                            || !hasBackpressureAfterProcessing  // 没有背压
                            || done;  // 上游已完成

                        if (shouldSend && e < r) {
                            // 有下游请求，发送容器
                            T mapped = mapper.apply(container);
                            a.onNext(mapped);
                            currentContainer = null;
                            e++;
                            r = requested;
                        }
                        // 否则保存容器状态，等待更多元素或下游请求
                    }
                }

                if (e != 0L) {
                    Operators.produced(REQUESTED, this, e);
                }

                if (cancelled) {
                    cleanup();
                    return;
                }

                if (done && queue.isEmpty()) {
                    terminate(a);
                    return;
                }

                // 如果还有请求且上游未完成，检查是否需要请求更多数据
                // 只有当 remainder 低于阈值时才请求，避免过度请求
                if (requested > 0 && !done && !cancelled) {
                    long rem = remainder;
                    // 如果 remainder 低于阈值（16），请求更多数据
                    if (rem < 16) {
                        long toRequest = queue.isEmpty() ? 32 : 16;
                        Operators.addCap(REMAINDER, this, toRequest);
                        this.s.request(toRequest);
                    }
                }

                missed = WIP.addAndGet(this, -missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void cleanup() {
            S element;
            while ((element = queue.poll()) != null) {
                releaseElement(element);
            }
            if (null != currentContainer) {
                Operators.onDiscard(currentContainer, currentContext());
            }
        }

        void releaseElement(S element) {
            if (onDrop != null) {
                onDrop.accept(element);
            } else {
                Operators.onDiscard(element, currentContext());
            }
        }

        /**
         * 终止流：发送错误或完成信号
         */
        void terminate(CoreSubscriber<? super T> a) {
            cleanup();
            Throwable ex = error;
            if (ex != null) {
                a.onError(ex);
            } else {
                a.onComplete();
            }
        }
    }


}
