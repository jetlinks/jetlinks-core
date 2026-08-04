package org.jetlinks.core.topic;

/**
 * {@link TopicRouteTable} 的有界只读统计快照。
 * <p>
 * 指标只描述 RouteTable 自身的注册、索引和更新，不包含消息投递、buffer 或 discard
 * 计数。
 *
 * @see TopicRouteTable#metrics()
 * @since 1.2.6
 */
public interface TopicRouteTableMetrics {

    /**
     * @return 当前未释放 Registration 数量
     * @since 1.2.6
     */
    long getRegistrationCount();

    /**
     * @return 当前 pattern group 数量
     * @since 1.2.6
     */
    long getPatternGroupCount();

    /**
     * @return 当前精确索引值与 Registration 关联数量
     * @since 1.2.6
     */
    long getIndexedValueCount();

    /**
     * @return 实际改变 Plan 的成功更新次数
     * @since 1.2.6
     */
    long getUpdateSuccessCount();

    /**
     * @return 更新失败次数
     * @since 1.2.6
     */
    long getUpdateFailureCount();
}
