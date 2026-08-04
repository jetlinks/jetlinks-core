package org.jetlinks.core.topic;

import org.jetlinks.core.lang.SeparatedCharSequence;
import reactor.core.Disposable;

/**
 * 一个目标在 {@link TopicRouteTable} 中的长期注册句柄。
 * <p>
 * 同一 Registration 的 updatePlan 与 dispose 必须线性化；dispose 后 matches 永远为
 * {@code false}，更新不能重新激活已释放句柄。
 *
 * @param <T> 关联目标类型
 * @see TopicRouteTable
 * @since 1.2.6
 */
public interface TopicRouteRegistration<T> extends Disposable {

    /**
     * @return 注册时关联的目标，不会返回 {@code null}
     * @since 1.2.6
     */
    T getTarget();

    /**
     * 返回当前 Plan 快照。
     *
     * @return 当前不可变 Route Plan，不会返回 {@code null}
     * @since 1.2.6
     */
    TopicSubscriptionPlan getPlan();

    /**
     * @return 当前 Plan revision；注册成功后的初始值为 {@code 0}
     * @since 1.2.6
     */
    long getRevision();

    /**
     * 同步原子替换当前完整 Route Plan。
     * <p>
     * 相同 Plan 不增加 revision；实际改变时 revision 加一。失败时抛出异常并保持旧
     * Plan，方法不执行阻塞 IO。getPlan 与 getRevision 必须来自同一原子快照。
     *
     * @param nextPlan 下一完整不可变 Plan，不能为 {@code null}
     * @return 更新后的当前 revision
     * @throws IllegalArgumentException Plan 或 Route 不受支持
     * @throws IllegalStateException Registration 已释放
     * @since 1.2.6
     */
    long updatePlan(TopicSubscriptionPlan nextPlan);

    /**
     * 使用当前快照执行最终匹配门禁。
     * <p>
     * 该方法是并发读热路径，不得执行阻塞 IO；dispose 后始终返回 {@code false}。
     *
     * @param topic 完整已分段 Topic，不能为 {@code null}
     * @return 当前未释放且 Plan 匹配时返回 {@code true}
     * @since 1.2.6
     */
    boolean matches(SeparatedCharSequence topic);
}
