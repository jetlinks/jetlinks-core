package org.jetlinks.core.event;

import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.Decoder;
import org.jetlinks.core.codec.Encoder;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 基于订阅发布的事件总线,可用于事件传递,消息转发等.
 *
 * @author zhouhao
 * @see 1.1
 * @see org.jetlinks.core.topic.Topic
 */
public interface EventBus {

    Flux<TopicPayload> subscribe(Subscription subscription);

    <T> Flux<T> subscribe(Subscription subscription, Decoder<T> type);

    <T> Mono<Integer> publish(String topic, Publisher<T> event);

    <T> Mono<Integer> publish(String topic, Encoder<T> encoder, Publisher<? extends T> eventStream);

    default <T> Flux<T> subscribe(Subscription subscription, Class<T> type) {
        return subscribe(subscription, Codecs.lookup(type));
    }

    default <T> Mono<Integer> publish(String topic, Encoder<T> encoder, T event) {
        return publish(topic, encoder, Mono.just(event));
    }

    @SuppressWarnings("all")
    default <T> Mono<Integer> publish(String topic, T event) {
        return publish(topic, (Encoder<T>) Codecs.lookup(event.getClass()), event);
    }

}
