package org.jetlinks.core.metadata;

/**
 * 字段在来源记录中的角色；字段标识、单位和值类型仍由原模型定义。
 * 线上使用字符串，未知新词由原模型保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum PropertySemanticRole {
    /** 来源记录的稳定身份字段；不将时间拼接值自动解释为不可变样本 ID。 */
    identity,
    /** 被观测的属性或现象标识，例如温度；区别于上报设备和结果值。 */
    observedProperty,
    /** 观测或计算得到的结果值，其类型和单位由字段模型定义。 */
    result,
    /** 原来源提供的数据质量，如有效、估算、缺失；不等同于业务状态。 */
    quality,
    /** 观测对应的业务时间，区别于平台入库时间。 */
    observedAt,
    /** 事件实际发生时间，例如一次告警发生。 */
    occurredAt,
    /** 活动或有效区间的开始时间。 */
    startedAt,
    /** 活动或有效区间的结束时间；缺失不表示已完成。 */
    endedAt,
    /** 来源记录被写入的时间，不能替代发生时间或观测时间。 */
    recordedAt,
    /** 原业务生命周期状态；具体值及完成含义仍由来源定义。 */
    status
}
