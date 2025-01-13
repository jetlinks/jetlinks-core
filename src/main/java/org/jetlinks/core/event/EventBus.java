package org.jetlinks.core.event;

import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.Decoder;
import org.jetlinks.core.codec.Encoder;
import org.jetlinks.core.codec.defaults.DirectCodec;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 基于订阅发布的事件总线,可用于事件传递,消息转发等.
 *
 * @author zhouhao
 * @see org.jetlinks.core.topic.Topic
 * @since 1.1
 */
public interface EventBus {

    /**
     * 从事件总线中订阅事件
     *
     * @param subscription 订阅信息
     * @return 事件流
     */
    Flux<TopicPayload> subscribe(Subscription subscription);

    /**
     * 从事件总线中订阅事件并指定handler来处理事件，通过调用{@link Disposable#dispose()}来取消订阅
     *
     * @param subscription 订阅信息
     * @return 事件流
     */
    default Disposable subscribe(Subscription subscription,
                                 Function<TopicPayload, Mono<Void>> handler) {
        return subscribe(subscription)
            .flatMap(handler)
            .subscribe();
    }

    /**
     * 从事件总线中订阅事件,并按照指定的解码器进行数据转换
     *
     * @param subscription 订阅信息
     * @param decoder      解码器
     * @param <T>          解码后结果类型
     * @return 事件流
     */
    @Deprecated
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
    @Deprecated
    <T> Mono<Long> publish(String topic, Encoder<T> encoder, Publisher<? extends T> eventStream);

    /**
     * 推送消息流，并指定编码器用于进行事件序列化
     *
     * @param topic       topic
     * @param encoder     编码器
     * @param eventStream 事件流
     * @param scheduler   调度器
     * @param <T>         void
     * @return 订阅者数量
     */
    @Deprecated
    <T> Mono<Long> publish(String topic,
                           Encoder<T> encoder,
                           Publisher<? extends T> eventStream,
                           Scheduler scheduler);

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
    @Deprecated
    default <T> Mono<Long> publish(String topic, Encoder<T> encoder, T event) {
        return publish(topic, encoder, Mono.just(event));
    }

    @Deprecated
    default <T> Mono<Long> publish(String topic, Encoder<T> encoder, T event, Scheduler scheduler) {
        return publish(topic, encoder, Mono.just(event), scheduler);
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

    /**
     * 使用CharSequence作为topic进行推送,
     * 可通过使用{@link org.jetlinks.core.lang.SharedPathString}提前构造topic来提升推送性能.
     *
     * @param topic topic
     * @param event 事件
     * @param <T>   事件类型
     * @return 订阅者数量
     * @see org.jetlinks.core.lang.SharedPathString
     * @since 1.2.3
     */
    default <T> Mono<Long> publish(CharSequence topic, T event) {
        return publish(topic.toString(), event);
    }

    /**
     * 使用CharSequence作为topic进行推送,
     * 可通过使用{@link org.jetlinks.core.lang.SharedPathString}提前构造topic来提升推送性能.
     * <p>
     * 注意: 如果没有订阅者,event将不会被订阅.适合按需推送等场景.
     *
     * @param topic topic
     * @param event 事件
     * @param <T>   事件类型
     * @return 订阅者数量
     * @see org.jetlinks.core.lang.SharedPathString
     * @since 1.2.3
     */
    default <T> Mono<Long> publish(CharSequence topic, Publisher<T> event) {
        return publish(topic.toString(), event);
    }

    /**
     * 使用CharSequence作为topic进行推送,
     * 可通过使用{@link org.jetlinks.core.lang.SharedPathString}提前构造topic来提升推送性能.
     * <p>
     * 注意: 如果没有订阅者,event将不会被订阅.适合按需推送等场景.
     *
     * <pre>{@code
     *   publish(topic, ()-> createData(...))
     * }</pre>
     *
     * @param topic topic
     * @param event 事件
     * @param <T>   事件类型
     * @return 订阅者数量
     * @see org.jetlinks.core.lang.SharedPathString
     * @see org.jetlinks.core.Lazy
     * @see RoutableSupplier
     * @since 1.2.3
     */
    default <T> Mono<Long> publish(CharSequence topic, Supplier<T> event) {
        return publish(topic, Mono.fromSupplier(event));
    }


    default <T> Mono<Long> publish(String topic, T event, Scheduler scheduler) {
        if (event instanceof Payload) {
            return publish(topic, ((Payload) event), scheduler);
        }
        return publish(topic, Codecs.lookup(event.getClass()), event, scheduler);
    }

    @Deprecated
    default Mono<Long> publish(String topic, Payload event) {
        return publish(topic, DirectCodec.INSTANCE, event);
    }

    @Deprecated
    default Mono<Long> publish(String topic, Payload event, Scheduler scheduler) {
        return publish(topic, DirectCodec.INSTANCE, event, scheduler);
    }
}
