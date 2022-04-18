package org.jetlinks.core.utils;

import reactor.core.publisher.Mono;

public interface Reactors {
    Mono<Boolean> ALWAYS_TRUE = Mono.just(true);
    Mono<Boolean> ALWAYS_FALSE = Mono.just(false);

    Mono<Integer> ALWAYS_ZERO = Mono.just(0);
    Mono<Integer> ALWAYS_ONE = Mono.just(1);

    Mono<Long> ALWAYS_ONE_LONG = Mono.just(1L);
    Mono<Long> ALWAYS_ZERO_LONG = Mono.just(0L);

}
