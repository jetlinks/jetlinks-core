package org.jetlinks.core.event;

import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.Decoder;
import org.jetlinks.core.codec.Encoder;
import org.jetlinks.core.codec.defaults.DirectCodec;
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

    /**
     * 从事件总线中订阅事件
     *
     * 特别注意!!!: 处理数据后需要手动调用释放，如:{@link TopicPayload#release()}
     *
     * @param subscription 订阅信息
     * @return 事件流
     */
    Flux<TopicPayload> subscribe(Subscription subscription);

    /**
     * 从事件总线中订阅事件,并按照指定的解码器进行数据转换
     *
     * @param subscription 订阅信息
     * @param decoder      解码器
     * @param <T>          解码后结果类型
     * @return 事件流
     */
    <T> Flux<T> subscribe(Subscription subscription, Decoder<T> decoder);

    /**
     * 推送消息流到事件总线,并返回有多少订阅者订阅了此topic,默认自动根据元素类型进行序列化
     *
     * @param topic topic
     * @param event 事件流
     * @param <T>   事件流元素类型
     * @return 订阅者数量
     */
    <T> Mono<Long> publish(String topic, Publisher<T> event);


    /**
     * 推送消息流，并指定编码器用于进行事件序列化
     *
     * @param topic       topic
     * @param encoder     编码器
     * @param eventStream 事件流
     * @param <T>         类型
     * @return 订阅者数量
     */
    <T> Mono<Long> publish(String topic, Encoder<T> encoder, Publisher<? extends T> eventStream);

    /**
     * 订阅主题并将事件数据转换为指定的类型
     *
     * @param subscription 订阅信息
     * @param type         类型
     * @param <T>          类型
     * @return 事件流
     */
    default <T> Flux<T> subscribe(Subscription subscription, Class<T> type) {
        return subscribe(subscription, Codecs.lookup(type));
    }

    /**
     * 推送单个数据到事件总线中,并指定编码器用于将事件数据进行序列化
     *
     * @param topic   主题
     * @param encoder 编码器
     * @param event   事件数据
     * @param <T>     事件类型
     * @return 订阅者数量
     */
    default <T> Mono<Long> publish(String topic, Encoder<T> encoder, T event) {
        return publish(topic, encoder, Mono.just(event));
    }

    /**
     * 推送单个数据到事件流中,默认自动根据事件类型进行序列化
     *
     * @param topic 主题
     * @param event 事件数据
     * @param <T>   事件类型
     * @return 订阅者数量
     */
    @SuppressWarnings("all")
    default <T> Mono<Long> publish(String topic, T event) {
        if (event instanceof Payload) {
            return publish(topic, ((Payload) event));
        }
        return publish(topic, Codecs.lookup(event.getClass()), event);
    }


    default Mono<Long> publish(String topic, Payload event) {
        return publish(topic, DirectCodec.INSTANCE, event);
    }

}
