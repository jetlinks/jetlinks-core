package org.jetlinks.core.monitor.limit;

import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

class NoopCircuitBreaker implements CircuitBreaker {

    static final NoopCircuitBreaker INSTANCE = new NoopCircuitBreaker();

    @Override
    public <R> Mono<R> execute(Callable<R> blockingCallable) {
        return Mono.fromCallable(blockingCallable);
    }

    @Override
    public <R> Mono<R> execute(Mono<R> asyncCallable) {
        return asyncCallable;
    }

    @Nullable
    @Override
    @SneakyThrows
    public <R> R executeBlocking(Callable<R> callable) {
        return callable.call();
    }
}
