package org.jetlinks.core.cluster;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 集群广播,用于向集群中发送广播消息
 *
 * @param <T>
 */
public interface ClusterTopic<T> {

    /**
     * 按通配符进行订阅,通配符支持 *, 如: message/*
     *
     * @return 消息流
     */
    Flux<TopicMessage<T>> subscribePattern();

    /**
     * 发送广播消息
     *
     * @param publisher 消息流
     * @return 接收到消息到订阅者数量
     */
    Mono<Integer> publish(Publisher<? extends T> publisher);

    /**
     * 订阅消息
     *
     * @return 消息流
     */
    default Flux<T> subscribe() {
        return subscribePattern()
                .map(TopicMessage::getMessage);
    }

    interface TopicMessage<T> {
        String getTopic();

        T getMessage();
    }

}
