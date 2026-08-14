package org.jetlinks.core.event;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
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
     * 使用结构化 Plan 创建可动态更新的事件流。
     * <p>
     * 具体实现应在返回的 EventStream 首次被订阅时激活路由。不支持结构化订阅的实现
     * 继承默认方法即可显式失败，不能退化为无约束 wildcard 或取消重建伪句柄。
     *
     * @param plan 初始完整订阅快照，不能为 {@code null}
     * @return 可更新、可取消的单下游事件流
     * @throws UnsupportedOperationException 当前实现不支持结构化订阅
     * @see SubscriptionPlan
     * @see EventStream
     * @since 1.2.6
     */
    default EventStream<TopicPayload> subscribe(SubscriptionPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan cannot be null");
        }
        throw new UnsupportedOperationException(
            "structured subscription plan is not supported"
        );
    }

    /**
     * 使用结构化 Plan 创建 handler 订阅。
     * <p>
     * handler 返回的 Mono 必须由实现组合进消息投递链，不能通过内部 subscribe 脱离
     * 生命周期启动。实现必须保留生产者 Reactor Context，使 handler 可通过
     * {@code Mono.deferContextual(...)} 延迟读取。
     *
     * @param plan 初始完整订阅快照，不能为 {@code null}
     * @param handler 非阻塞处理器，不能为 {@code null}，也不能返回 {@code null}
     * @return 可更新、可取消的订阅句柄
     * @throws UnsupportedOperationException 当前实现不支持结构化订阅
     * @see EventSubscription
     * @since 1.2.6
     */
    default EventSubscription subscribe(
        SubscriptionPlan plan,
        Function<TopicPayload, Mono<Void>> handler) {
        if (plan == null) {
            throw new IllegalArgumentException("plan cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler cannot be null");
        }
        throw new UnsupportedOperationException(
            "structured subscription plan is not supported"
        );
    }

    /**
     * 从事件总线中订阅事件
     *
     * @param subscription 订阅信息
     * @return 事件流
     */
    Flux<TopicPayload> subscribe(Subscription subscription);

    /**
     * 从事件总线中订阅事件并指定handler来处理事件，通过调用{@link Disposable#dispose()}来取消订阅。
     * <p>
     * 如果配置了{@link Subscription.Feature#persistent},则需要调用{@link Cancelable#cancel()}来取消订阅。
     *
     * @param subscription 订阅信息
     * @return 事件流
     */
    Cancelable subscribe(Subscription subscription,
                         Function<TopicPayload, Mono<Void>> handler);

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
     * 订阅主题并将事件数据转换为指定的类型
     *
     * @param subscription 订阅信息
     * @param type         类型
     * @param <T>          类型
     * @return 事件流
     */
    <T> Flux<T> subscribe(Subscription subscription, Class<T> type);

    /**
     * 订阅主题并将事件数据转换为指定的类型
     *
     * @param subscription 订阅信息
     * @param mapper       类型
     * @param <T>          类型
     * @return 事件流
     */
    default <T> Flux<T> subscribe(Subscription subscription, BiFunction<ContextView, TopicPayload, T> mapper) {
        return subscribe(subscription)
            .mapNotNull(topic -> mapper.apply(Context.empty(), topic));
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
    <T> Mono<Long> publish(String topic, T event);

    /**
     * 向多个 Topic 推送同一个事件，并返回所有 Topic 的逻辑订阅者数量之和。
     *
     * <p>Topic 集合会在调用时复制并校验，空集合返回 {@code 0}。默认实现按 Topic
     * 独立调用既有单 Topic API；具体实现可以提供更高效的批量路径。重复
     * Topic 按输入元素分别推送，同一订阅者命中多个 Topic 时也分别计数和投递。本方法
     * 不提供事务回滚保证，部分 Topic 成功后其他 Topic 失败时不会撤销已完成的投递。
     *
     * @param topics Topic 集合，不能为 {@code null}，元素不能为 {@code null}；调用时会复制快照
     * @param event 事件对象；同一对象引用可被多个 Topic 使用，允许为 {@code null}
     * @param <T> 事件类型
     * @return 所有 Topic 的逻辑订阅者数量之和
     * @since 1.3.2
     * @see #publish(Collection, Supplier)
     * @see #publish(Collection, Publisher)
     */
    default <T> Mono<Long> publish(Collection<? extends CharSequence> topics, T event) {
        List<CharSequence> snapshot = snapshotTopics(topics);
        return Flux
            .fromIterable(snapshot)
            .flatMap(topic -> publish(topic, event))
            .reduce(0L, Long::sum);
    }

    /**
     * 向多个 Topic 推送一个惰性事件生产器。
     *
     * <p>默认实现延迟执行一次 Supplier，再通过对象批量重载 fan-out，确保包括
     * {@code null} 在内的生产结果与对象重载语义一致。空 Topic 集合不执行 Supplier；
     * 默认实现无法预先判断非空 Topic 的逻辑订阅者，因此即使最终没有订阅者也可能执行
     * Supplier；需要按候选延迟执行的实现应覆盖此方法。其他计数、重复 Topic、错误、取消
     * 和非事务语义与
     * {@link #publish(Collection, Object)} 相同。
     *
     * @param topics Topic 集合，不能为 {@code null}，元素不能为 {@code null}
     * @param event 惰性事件生产器，不能为 {@code null}；生产结果允许为 {@code null}
     * @param <T> 事件类型
     * @return 所有 Topic 的逻辑订阅者数量之和
     * @since 1.3.2
     */
    default <T> Mono<Long> publish(Collection<? extends CharSequence> topics, Supplier<T> event) {
        List<CharSequence> snapshot = snapshotTopics(topics);
        Objects.requireNonNull(event, "event cannot be null");
        if (snapshot.isEmpty()) {
            return Mono.just(0L);
        }
        return Mono.defer(() -> publish(snapshot, event.get()));
    }

    /**
     * 向多个 Topic 推送一个事件流。
     *
     * <p>默认实现按 Topic 独立委托既有单 Topic API，因此事件流可能被订阅多次；事件源
     * 不支持重复订阅，或需要单次订阅源、候选快照和延迟消费保证的实现应覆盖此方法。
     * 返回值按 Topic 的逻辑订阅者数量求和，不会因事件流元素数量重复累加。其他计数、
     * 重复 Topic、错误、取消和非事务语义与 {@link #publish(Collection, Object)} 相同。
     *
     * @param topics Topic 集合，不能为 {@code null}，元素不能为 {@code null}
     * @param event 事件流，不能为 {@code null}
     * @param <T> 事件类型
     * @return 所有 Topic 的逻辑订阅者数量之和
     * @since 1.3.2
     */
    default <T> Mono<Long> publish(Collection<? extends CharSequence> topics, Publisher<T> event) {
        List<CharSequence> snapshot = snapshotTopics(topics);
        Objects.requireNonNull(event, "event cannot be null");
        if (snapshot.isEmpty()) {
            return Mono.just(0L);
        }
        return Flux
            .fromIterable(snapshot)
            .flatMap(topic -> publish(topic, event))
            .reduce(0L, Long::sum);
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

    /**
     * 使用指定的调度器来推送事件
     *
     * @param topic     主题
     * @param event     事件数据
     * @param scheduler 调度器
     * @param <T>       事件类型
     * @return 订阅者数量
     */
    <T> Mono<Long> publish(String topic, T event, Scheduler scheduler);

    /**
     * 在默认实现中复制 Topic 集合，避免异步发布链持有调用方后续可能修改的集合。
     */
    static List<CharSequence> snapshotTopics(Collection<? extends CharSequence> topics) {
        Objects.requireNonNull(topics, "topics cannot be null");
        List<CharSequence> snapshot = new ArrayList<>(topics.size());
        for (CharSequence topic : topics) {
            snapshot.add(Objects.requireNonNull(topic, "topic cannot be null"));
        }
        return snapshot;
    }
}
