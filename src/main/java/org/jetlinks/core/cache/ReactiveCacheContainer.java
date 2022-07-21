package org.jetlinks.core.cache;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

public interface ReactiveCacheContainer<T> extends Disposable {

    Mono<T> compute(String key, BiFunction<String, T, Mono<T>> compute);

    Mono<T> get(String key, Mono<T> defaultValue);

    T getNow(String key);

    T remove(String key);

    static <T> ReactiveCacheContainer<T> create() {
        return new DefaultReactiveCacheContainer<>();
    }
}
