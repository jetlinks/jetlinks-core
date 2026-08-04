package org.jetlinks.core.event;

import reactor.core.publisher.Flux;

/**
 * 同时暴露事件 Flux 和更新句柄的结构化订阅。
 * <p>
 * EventStream 在 {@link #flux()} 被订阅时激活；同一实例只允许一个下游，取消 Flux
 * 必须与释放句柄共享生命周期。
 *
 * @param <T> 事件元素类型
 * @see EventSubscription
 * @see EventBus#subscribe(SubscriptionPlan)
 * @since 1.2.6
 */
public interface EventStream<T> extends EventSubscription {

    /**
     * 返回单下游事件流。第二个下游必须收到 duplicate subscription 错误。
     * <p>
     * 流取消后不得额外发送完成信号；背压、discard 和生产者 Context 由具体 EventBus
     * 实现保持。
     *
     * @return 与句柄共享 Plan 和取消生命周期的事件流
     * @since 1.2.6
     */
    Flux<T> flux();
}
