package org.jetlinks.core.metadata;

/**
 * 说明整个模型描述哪一类数据，区别于 PropertySemanticRole 对单个字段用途的说明。
 * 例如设备是 resource，温度快照可同时是 observation 和 result，单次告警发生是 event。
 * 这些分类可以组合；声明分类不会自动创建存储、订阅或执行能力，自定义词仍保留在原模型中。
 * 这是平台词汇，不与 SOSA/PROV 类自动等同：这里的 observation 描述观测数据，
 * SOSA 的 Observation 则是进行测量的活动；只有来源提供活动及输入输出证据时才建立相应关系。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum SemanticType {
    /** 可以用稳定身份引用的业务事物，例如设备、产品、组织。 */
    resource,
    /** 描述某个对象的测量或统计，例如某台设备 10:00 的温度、过去一小时的平均温度。 */
    observation,
    /** 某次事情确实发生的记录，例如一次设备告警；不是告警规则的定义。 */
    event,
    /** 有执行过程的一次活动，例如一次通知发送尝试；重试是另一尝试，不是通知动作的定义。 */
    activity,
    /** 测量或活动得到的输出，例如属性快照、指标计算结果；有输出不等于业务已成功完成。 */
    result
}
