package org.jetlinks.core.metadata;

/**
 * 模型所描述数据的语义角色，可组合使用；不表示互斥的业务类型。
 * 线上使用字符串，未知新词由原模型保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum SemanticType {
    /** 具有稳定身份的资源或业务主体。 */
    resource,
    /** 对主体某属性在特定时点或区间的观测。 */
    observation,
    /** 某次发生的事实，例如单次告警发生。 */
    event,
    /** 一次具有执行过程的活动，例如通知发送尝试。 */
    activity,
    /** 活动产生的输出或业务结果，不自动证明业务完成。 */
    result
}
