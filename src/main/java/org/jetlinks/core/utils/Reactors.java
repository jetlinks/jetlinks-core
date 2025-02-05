package org.jetlinks.core.utils;

import com.google.common.util.concurrent.RateLimiter;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;
import reactor.util.context.ContextView;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public interface Reactors {
    Mono<Boolean> ALWAYS_TRUE = Mono.just(true);
    Mono<Boolean> ALWAYS_FALSE = Mono.just(false);

    Mono<Integer> ALWAYS_ZERO = Mono.just(0);
    Mono<Integer> ALWAYS_ONE = Mono.just(1);

    Mono<Long> ALWAYS_ONE_LONG = Mono.just(1L);
    Mono<Long> ALWAYS_ZERO_LONG = Mono.just(0L);

    Sinks.EmitFailureHandler RETRY_NON_SERIALIZED = (signal, failure) -> failure == Sinks.EmitResult.FAIL_NON_SERIALIZED;

    static Sinks.EmitFailureHandler emitFailureHandler() {
        return RETRY_NON_SERIALIZED;
    }

    static Sinks.EmitFailureHandler retryNonSerialized() {
        return RETRY_NON_SERIALIZED;
    }

    static <T> Sinks.Many<T> createMany(int bufferSize, boolean autoCancel) {
        return Sinks
            .many()
            .multicast()
            .onBackpressureBuffer(bufferSize, autoCancel);
    }

    static <T> Sinks.Many<T> createMany(boolean autoCancel) {
        return createMany(Queues.SMALL_BUFFER_SIZE, autoCancel);
    }

    static <T> Sinks.Many<T> createMany() {
        return createMany(false);
    }


    static <T> Flux<T> doWhenContext(Predicate<ContextView> predicate, Flux<T> flux) {
        return Flux.deferContextual(ctx -> {
            if (predicate.test(ctx)) {
                return flux;
            }
            return Flux.empty();
        });
    }

    static <T> Mono<T> doWhenContext(Predicate<ContextView> predicate, Mono<T> mono) {
        return Mono.deferContextual(ctx -> {
            if (predicate.test(ctx)) {
                return mono;
            }
            return Mono.empty();
        });
    }

    /**
     * 阻塞获取Mono中的值,如果在响应式操作中执行此逻辑,建议使用{@link Schedulers#boundedElastic()}.
     *
     * @param mono    Mono对象
     * @param timeout 超时时间
     * @param <T>
     * @return Mono的值
     */
    @SneakyThrows
    @SuppressWarnings("all")
    static <T> T blockGet(Mono<T> mono, Duration timeout) {

        if (mono instanceof Callable) {
            return (T) ((Callable<?>) mono).call();
        }

        //在非阻塞线程中,使用toFuture处理.
        if (Schedulers.isNonBlockingThread(Thread.currentThread())) {
            CompletableFuture<T> future = mono.toFuture();
            try {
                return future.get(timeout.getNano(), TimeUnit.NANOSECONDS);
            } finally {
                future.cancel(true);
            }
        }

        return mono.block(timeout);

    }
}
