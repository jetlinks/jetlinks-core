package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SeparatedCharSequence;

import java.util.function.Consumer;

/**
 * Topic Route 的并发候选索引 SPI。
 * <p>
 * RouteTable 只负责同步、内存内的候选定位，不执行消息投递、网络同步或响应式编排。
 * 交付前仍须调用 {@link TopicRouteRegistration#matches(SeparatedCharSequence)} 执行当前
 * Plan 的最终门禁；实现不能在每次查找时扫描全部 Registration。
 *
 * @param <T> Registration 关联目标类型
 * @see TopicRouteRegistration
 * @see TopicSubscriptionPlan
 * @since 1.2.6
 */
public interface TopicRouteTable<T> {

    /**
     * 注册目标及其初始完整 Route Plan。
     * <p>
     * 调用在当前线程同步完成，不允许执行阻塞 IO；空 Plan 合法，表示句柄存在但不会
     * 产生候选。未知 Route 实现必须显式失败。
     *
     * @param target 关联目标，不能为 {@code null}
     * @param plan 初始不可变 Plan，不能为 {@code null}
     * @return 独立、可更新、可释放的 Registration
     * @throws IllegalArgumentException target、Plan 或 Route 不受支持
     * @since 1.2.6
     */
    TopicRouteRegistration<T> register(T target, TopicSubscriptionPlan plan);

    /**
     * 使用已分段 Topic 同步查找候选 Registration。
     * <p>
     * 同一 Registration 即使被多个 Route 命中，也只能回调一次。consumer 异常沿
     * 当前调用栈传播，RouteTable 不重试也不吞掉异常。
     *
     * @param topic 完整已分段 Topic，不能为 {@code null}
     * @param consumer 候选接收器，不能为 {@code null}
     * @since 1.2.6
     */
    void find(SeparatedCharSequence topic,
              Consumer<? super TopicRouteRegistration<T>> consumer);

    /**
     * 使用 CharSequence Topic 同步查找候选 Registration。
     * <p>
     * 实现应复用与已分段重载相同的索引和去重语义，不得退化为全量扫描。
     *
     * @param topic 完整 Topic，不能为 {@code null}
     * @param consumer 候选接收器，不能为 {@code null}
     * @since 1.2.6
     */
    void find(CharSequence topic,
              Consumer<? super TopicRouteRegistration<T>> consumer);

    /**
     * 返回路由表的有界只读统计快照。
     * <p>
     * 统计只描述 Registration、索引和 Plan 更新，不包含 EventBus 消息投递计数。
     *
     * @return 统计快照，不会返回 {@code null}，且不暴露内部集合
     * @since 1.2.6
     */
    TopicRouteTableMetrics metrics();
}
