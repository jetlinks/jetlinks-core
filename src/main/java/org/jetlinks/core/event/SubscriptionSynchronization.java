package org.jetlinks.core.event;

/**
 * 订阅 Plan 更新完成时的外部同步状态。
 * <p>
 * 所有状态都表示本地 Plan 已经生效，不暴露尚未完成本地切换的中间状态。
 *
 * @see SubscriptionUpdateResult
 * @since 1.2.6
 */
public enum SubscriptionSynchronization {

    /** 本地已切换，且该订阅不需要外部同步。 */
    LOCAL_APPLIED,

    /** 本地已切换，实现声明要求的外部订阅状态也已同步。 */
    CLUSTER_SYNCHRONIZED,

    /** 本地已切换，但外部同步未完全成功，恢复方式由具体实现定义。 */
    DEGRADED
}
