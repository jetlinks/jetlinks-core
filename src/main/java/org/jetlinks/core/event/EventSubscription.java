package org.jetlinks.core.event;

import reactor.core.publisher.Mono;

/**
 * 可动态更新的 EventBus 订阅生命周期句柄。
 * <p>
 * 实现必须以完整不可变快照作为更新边界，并保证 updatePlan 与 dispose 对同一句柄
 * 具有确定的线性化顺序。释放后不得被迟到更新复活。
 *
 * @see SubscriptionPlan
 * @see EventStream
 * @since 1.2.6
 */
public interface EventSubscription extends Cancelable {

    /**
     * 返回当前本地已经生效的不可变订阅快照。
     *
     * @return 当前 Plan，不会返回 {@code null}
     * @since 1.2.6
     */
    SubscriptionPlan getPlan();

    /**
     * 原子替换完整 Plan。
     * <p>
     * {@link SubscriptionPlan#isUpdateCompatibleWith(SubscriptionPlan)} 必须返回
     * {@code true}；返回的 Mono 被多次订阅时不得重复推进 revision 或重复修改路由。
     * Mono 完成时本地切换必须已经生效，错误时旧 Plan 保持有效。
     *
     * @param nextPlan 下一完整快照，不能为 {@code null}
     * @return 被多次订阅时仍只执行一次更新语义的结果 Mono
     *         ；Plan 非法或固定属性变化时以 {@link IllegalArgumentException} 结束，
     *         句柄已释放时以 {@link IllegalStateException} 结束
     * @since 1.2.6
     */
    Mono<SubscriptionUpdateResult> updatePlan(SubscriptionPlan nextPlan);
}
