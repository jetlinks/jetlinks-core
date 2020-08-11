package org.jetlinks.core.message.codec.context;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 串行上下文, 可并行输入,串行输出.
 *
 * @param <IN>  输入
 * @param <OUT> 输出
 */
public interface SerialContext<IN, OUT> {

    static <IN, OUT> SerialContext<IN, OUT> newContext() {
        return new QueueSerialContext<>();
    }

    /**
     * 输入一个数据,然后等待输出
     *
     * @param in      输入
     * @param timeout 超时时间
     * @return 输出
     */
    Mono<OUT> inputAndAwait(IN in, Duration timeout);

    /**
     * 输出一个结果
     *
     * @param out 输出
     */
    void output(OUT out);

    /**
     * 监听输入
     *
     * @return 输入流
     */
    Flux<IN> listen();
}
