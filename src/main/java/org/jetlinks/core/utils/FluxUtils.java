package org.jetlinks.core.utils;

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
import java.util.concurrent.TimeUnit;

public class FluxUtils {

    public static <T> Flux<List<T>> bufferRate(Flux<T> flux, int rate, Duration maxTimeout) {
        return bufferRate(flux, rate, 100, maxTimeout);
    }

    public static <T> Flux<List<T>> bufferRate(Flux<T> flux, int rate, int maxSize, Duration maxTimeout) {
        return Flux.create(sink -> {
            BufferRateSubscriber<T> subscriber = new BufferRateSubscriber<>(sink, maxSize, rate, maxTimeout);

            flux.elapsed().subscribe(subscriber);

            sink.onDispose(subscriber);
        });

    }

    static class BufferRateSubscriber<T> extends BaseSubscriber<Tuple2<Long, T>> {
        int bufferSize;
        int rate;

        List<T> bufferArray;
        FluxSink<List<T>> sink;

        Duration timeout;
        Scheduler timer = Schedulers.parallel();
        Disposable timerDispose;

        BufferRateSubscriber(FluxSink<List<T>> sink, int bufferSize, int rate, Duration timeout) {
            this.sink = sink;
            this.bufferSize = bufferSize;
            this.rate = rate;
            this.timeout = timeout;
            newBuffer();
        }

        protected List<T> newBuffer() {
            List<T> buffer = bufferArray;
            bufferArray = new ArrayList<>(bufferSize);
            return buffer;
        }

        @Override
        protected void hookFinally(@Nonnull SignalType type) {
            if (null != timerDispose) {
                timerDispose.dispose();
            }
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
                if (bufferArray.size() >= bufferSize) {
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
