package org.jetlinks.core.cluster;

import reactor.core.publisher.Mono;

public interface ClusterCounter {

    /**
     * 递增 n
     *
     * @param delta 增量
     * @return 最新值
     */
    Mono<Double> increment(double delta);

    /**
     * 递增 1
     *
     * @return 自增后的值
     */
    default Mono<Double> increment() {
        return increment(1D);
    }

    /**
     * 递减 1
     *
     * @return 递减后等值
     */
    default Mono<Double> decrement() {
        return decrement(1D);
    }

    /**
     * 递减
     *
     * @param delta 减量
     * @return 最新值
     */
    default Mono<Double> decrement(double delta) {
        return increment(-delta);
    }


    /**
     * 获取当前值
     *
     * @return 当前值
     */
    Mono<Double> get();

    /**
     * 设置值
     *
     * @param value 新的值
     * @return 旧的值
     */
    Mono<Double> set(double value);

    /**
     * 设置值并返回最新的值
     *
     * @param value 值
     * @return 最新的值
     */
    Mono<Double> setAndGet(double value);

    /**
     * 获取值然后更新
     *
     * @param value 值
     * @return 更新前的值
     */
    Mono<Double> getAndSet(double value);

}
