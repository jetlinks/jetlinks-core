package org.jetlinks.core.cluster;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ClusterSet<T> {

    Mono<Boolean> add(T value);

    Mono<Boolean> add(Collection<T> values);

    Mono<Boolean> remove(T value);

    Mono<Boolean> remove(Collection<T> values);

    Flux<T> values();

}
