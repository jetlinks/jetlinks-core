package org.jetlinks.core.cluster;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

/**
 * 集群缓存,通常用于集群见共享数据.
 *
 * @param <K> Key
 * @param <V> Value
 * @author zhouhao
 * @since 1.0
 */
public interface ClusterCache<K, V> {

    /**
     * 根据Key获取值,值不存在时返回{@link Mono#empty()}
     *
     * @param key Key
     * @return 值
     */
    Mono<V> get(K key);

    /**
     * 批量获取缓存
     *
     * @param key key集合
     * @return 键值对
     */
    Flux<Map.Entry<K, V>> get(Collection<K> key);

    /**
     * 设置值
     *
     * @param key   key
     * @param value value
     * @return 是否成功
     */
    Mono<Boolean> put(K key, V value);

    /**
     * 设置值,如果值以及存在则忽略.
     *
     * @param key   key
     * @param value value
     * @return 是否成功
     */
    Mono<Boolean> putIfAbsent(K key, V value);

    /**
     * 根据key删除缓存
     *
     * @param key key
     * @return 是否删除成功
     */
    Mono<Boolean> remove(K key);

    /**
     * 获取值然后删除
     * @param key key
     * @return value
     */
    Mono<V> getAndRemove(K key);

    /**
     * 批量删除缓存
     *
     * @param key key
     * @return 是否删除成
     */
    Mono<Boolean> remove(Collection<K> key);

    /**
     * 判断缓存中是否包含key
     *
     * @param key key
     * @return 是否包含key
     */
    Mono<Boolean> containsKey(K key);

    /**
     * 获取缓存的所有key
     *
     * @return key流
     */
    Flux<K> keys();

    /**
     * 获取缓存的所有值
     *
     * @return value 流
     */
    Flux<V> values();

    /**
     * 批量设置值
     *
     * @param multi 批量缓存
     * @return 是否成功
     */
    Mono<Boolean> putAll(Map<? extends K, ? extends V> multi);

    /**
     * @return 缓存数量
     */
    Mono<Integer> size();

    /**
     * @return 所有键值对
     */
    Flux<Map.Entry<K, V>> entries();

    /**
     * 清空缓存
     *
     * @return 清空结果
     */
    Mono<Void> clear();
}
