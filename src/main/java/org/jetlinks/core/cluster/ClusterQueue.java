package org.jetlinks.core.cluster;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ClusterQueue<T> {

    Flux<T> subscribe();

    Mono<T> poll();

    Mono<Boolean> add(Publisher<T> publisher);

    Mono<Boolean> addBatch(Publisher<? extends Collection<T>> publisher);

    void setLocalConsumerPercent(float percent);

    void stop();

    Mono<Integer> size();
}
