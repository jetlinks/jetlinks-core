package org.jetlinks.core.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.NonNull;
import org.checkerframework.checker.index.qual.NonNegative;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Function;

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
           return flux -> DistinctDurationFlux.create(flux,keySelector,duration);
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
            if (bufferArray.size() > 0) {
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

}
