package org.jetlinks.core.cluster;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClusterTopic<T> {

    Flux<TopicMessage<T>> subscribePattern();

    default Flux<T> subscribe() {
        return subscribePattern()
                .map(TopicMessage::getMessage);
    }

    Mono<Integer> publish(Publisher<? extends T> publisher);

    interface TopicMessage<T> {
        String getTopic();

        T getMessage();
    }

}
