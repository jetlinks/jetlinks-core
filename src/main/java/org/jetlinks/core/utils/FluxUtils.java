package org.jetlinks.core.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
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

        private final Supplier<C> containerSupplier;
        private final BiFunction<C, S, C> merger;
        private final Predicate<C> bufferPredicate;
        private final Function<C, T> mapper;
        private final Consumer<S> onDrop;
        private final CoreSubscriber<? super T> actual;

        private volatile long requested;
        private volatile int wip;

        private volatile boolean done;
        private volatile Throwable error;
        private volatile boolean cancelled;

        private Subscription s;

        private final Queue<S> queue = Queues.<S>unboundedMultiproducer().get();

        private C currentContainer; // 当前正在累积的容器

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
                    boolean containerWasEmpty = currentContainer == null;
                    boolean hasMerged = false;
                    boolean shouldEmit = false;

                    // 合并数据，直到bufferPredicate返回false或队列为空
                    while (!queue.isEmpty()) {
                        S element = queue.peek();
                        if (element == null) {
                            break;
                        }

                        // 尝试合并元素
                        // 注意：merger可能会修改原容器，所以我们需要确保merger不修改原容器
                        // 或者merger返回新容器。如果merger修改了原容器，这是实现问题。
                        C newContainer = merger.apply(container, element);

                        // 使用bufferPredicate判断是否可以继续缓冲
                        boolean canBuffer = bufferPredicate.test(newContainer);
                        if (canBuffer) {
                            // 可以继续缓冲，合并元素
                            queue.poll(); // 确认消费元素
                            container = newContainer;
                            hasMerged = true;
                            containerWasEmpty = false;
                        } else {
                            // 不能再缓冲了
                            // 如果容器是空的（还没有合并任何元素），仍然合并这个元素（避免无限循环）
                            if (containerWasEmpty) {
                                queue.poll();
                                container = newContainer;
                                hasMerged = true;
                            } else if (done) {
                                // 上游已完成，强制合并剩余元素（确保流能结束）
                                queue.poll();
                                container = newContainer;
                                hasMerged = true;
                            } else {
                                // 容器不为空且上游未完成，不合并这个元素
                                // 注意：如果merger修改了原容器，此时container可能已经被修改了
                                // 但根据接口约定，merger应该返回新容器而不修改原容器
                                // 如果merger修改了原容器，这是merger实现的问题
                                // 我们不使用newContainer，保持container不变
                                // 元素保留在队列中，等待下次处理
                            }
                            // 标记应该发送（因为bufferPredicate返回false）
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
                    // 1. bufferPredicate返回false（不能再缓冲了）且容器不为空 -> 必须发送
                    // 2. 没有背压且合并了元素 -> 立即发送（避免延迟）
                    // 3. 上游已完成且合并了元素 -> 立即发送（确保流能结束）
                    // 4. 有背压且上游未完成 -> 保存容器状态，等待更多元素
                    if (shouldEmit && !containerWasEmpty) {
                        // bufferPredicate返回false，不能再缓冲，必须发送（容器不为空）
                        T mapped = mapper.apply(container);
                        a.onNext(mapped);
                        currentContainer = null;
                        e++;
                        r = requested;
                    } else if (hasMerged && (!hasBackpressureAfterProcessing || done) && !shouldEmit) {
                        // 没有背压或上游已完成，且bufferPredicate还允许继续缓冲，立即发送（避免延迟）
                        T mapped = mapper.apply(container);
                        a.onNext(mapped);
                        currentContainer = null;
                        e++;
                        r = requested;
                    }
                    // 如果有背压且上游未完成，且合并了元素但bufferPredicate还允许继续缓冲，队列为空
                    // 保存容器状态，等待更多元素，不发送
                    // currentContainer已经在上面的hasMerged块中设置了
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

                missed = WIP.addAndGet(this, -missed);
                if (missed == 0) {
                    break;
                }
            }

            // 如果还有请求且上游未完成，继续请求上游数据
            if (requested > 0 && !done && !cancelled) {
                // 如果队列为空，请求更多数据以保持数据流
                // 如果队列不为空，也请求一些数据以预取（提高性能）
                long toRequest = queue.isEmpty() ? 256 : 1;
                this.s.request(toRequest);
            }
        }

        void cleanup() {
            S element;
            while ((element = queue.poll()) != null) {
                releaseElement(element);
            }
        }

        void releaseElement(S element) {
            if (onDrop != null) {
                onDrop.accept(element);
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
