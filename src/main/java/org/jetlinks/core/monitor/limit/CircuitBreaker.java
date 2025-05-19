package org.jetlinks.core.monitor.limit;

import org.jetlinks.core.Wrapper;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

public interface CircuitBreaker extends Wrapper {

    static CircuitBreaker noop() {
        return NoopCircuitBreaker.INSTANCE;
    }

    /**
     * 执行阻塞式任务并返回异步结果,当熔断器已经熔断时,则可能返回错误{@link CircuitBreakerOpenedException}
     *
     * @param blockingCallable 阻塞式任务
     * @param <R>              结果类型
     * @return 异步结果
     * @see DoNotCircuitBreakerException
     */
    <R> Mono<R> execute(Callable<R> blockingCallable);

    /**
     * 执行异步任务 ,当熔断器已经熔断时,则可能返回错误{@link CircuitBreakerOpenedException}
     *
     * @param asyncCallable 异步任务
     * @param <R>           结果类型
     * @return 异步结果
     * @see CircuitBreakerOpenedException
     * @see DoNotCircuitBreakerException
     */
    <R> Mono<R> execute(Mono<R> asyncCallable);

    /**
     * 执行阻塞式任务并返回结果,当熔断器已经熔断时,则可能返回错误{@link CircuitBreakerOpenedException}
     *
     * @param callable 任务
     * @param <R>      结果
     * @return 返回结果
     */
    @Nullable
    <R> R executeBlocking(Callable<R> callable);

}
