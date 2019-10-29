package org.jetlinks.core.cluster;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

public interface ClusterCache<K,V> {
    Mono<V> get(K key);

    Flux<V> get(Collection<K> key);

    Mono<Boolean> put(K key, V value);

    Mono<Boolean> putIfAbsent(K key, V value);

    Mono<Boolean> remove(K key);

    Mono<Boolean> remove(Collection<K> key);

    Mono<Boolean> containsKey(K key);

    Flux<K> keys();

    Flux<V> values();

    Mono<Boolean> putAll(Map<? extends K, ? extends V> multi);

    Mono<Integer> size();

    Flux<Map.Entry<K,V>> entries();

    Mono<Void> clear();
}
