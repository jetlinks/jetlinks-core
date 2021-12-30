package org.jetlinks.core.cluster;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 集群Set支持
 *
 * @param <T> 元素类型
 * @author zhouhao
 * @since 1.1.3
 */
public interface ClusterSet<T> {

    /**
     * 添加数据到Set中
     *
     * @param value 数据
     * @return true 成功 false 失败
     */
    Mono<Boolean> add(T value);

    /**
     * 添加多个数据到Set中
     *
     * @param values 数据集
     * @return true 成功 false 失败
     */
    Mono<Boolean> add(Collection<T> values);

    /**
     * 从Set中删除数据
     *
     * @param value 要删除到数据
     * @return true 成功 false 失败
     */
    Mono<Boolean> remove(T value);

    /**
     * 从Set中删除多个数据
     *
     * @param values 要删除到数据集
     * @return true 成功 false 失败
     */
    Mono<Boolean> remove(Collection<T> values);

    /**
     * 获取Set中到全部数据
     *
     * @return 数据流
     */
    Flux<T> values();

}
