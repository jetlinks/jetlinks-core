package org.jetlinks.core.cache;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ReactiveCacheContainer<K, V> extends Disposable {

    Mono<V> compute(K key, BiFunction<K, V, Mono<V>> compute);

    Mono<V> computeIfAbsent(K key, Function<K, Mono<V>> compute);

    Mono<V> get(K key, Mono<V> defaultValue);

    V put(K key, V value);

    boolean containsKey(K key);

    V getNow(K key);

    V remove(K key);

    Flux<V> values();

    List<V> valuesNow();

    void clear();

    static <K, T> ReactiveCacheContainer<K, T> create() {
        return new DefaultReactiveCacheContainer<>();
    }
}
