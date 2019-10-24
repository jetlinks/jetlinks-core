package org.jetlinks.core.cluster;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClusterTopic<T> {

    Flux<T> subscribe();

    Mono<Integer> publish(Publisher<? extends T> publisher);

}
