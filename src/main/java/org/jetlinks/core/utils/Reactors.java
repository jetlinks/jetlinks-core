package org.jetlinks.core.utils;

import reactor.core.publisher.Mono;

public interface Reactors {
    Mono<Boolean> ALWAYS_TRUE = Mono.just(true);
    Mono<Boolean> ALWAYS_FALSE = Mono.just(false);

    Mono<Integer> ALWAYS_ONE = Mono.just(1);

}
