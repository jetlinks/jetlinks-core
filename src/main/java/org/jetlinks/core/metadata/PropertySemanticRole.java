package org.jetlinks.core.metadata;

/**
 * 说明一条业务记录中的每个字段表示什么，字段名称、值类型和单位仍使用原属性模型。
 * 例如设备温度快照中，deviceId 是 subjectId，value 是 result，timestamp 是 observedAt；
 * 若快照另有自身的记录 ID，该字段才是 identity。角色名称按字符串传输，自定义词保留在原模型中。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum PropertySemanticRole {
    /** 当前模型所述实体自身的 ID，例如设备或告警记录 ID；不能拿关联设备 ID 代替记录身份。 */
    identity,
    /** 业务编号，例如资产编号或序列号；不因此声明唯一性，也不取代对象自身的身份。 */
    identifier,
    /** 这条记录描述谁，例如设备温度快照中的 deviceId；对象类型仍由查询上下文提供。 */
    subjectId,
    /** 当前数据采用哪个业务模型，例如设备快照中的 productId；对象类型仍是 device，不是产品 ID。 */
    modelId,
    /** 测量或计算的属性 ID，例如 temperature；同层唯一 result 才可关联，多个结果应各放一条记录。 */
    observedProperty,
    /** 实际测量或计算的值，例如温度 21、告警数量 0；类型和单位使用原属性定义。 */
    result,
    /** 现有快照使用的结果值别名；统一读取为 result，新声明优先使用 result。 */
    @Deprecated
    value,
    /** 同层唯一结果采用的原 PropertyMetadata 或其 JSON，例如 temperature 的类型、单位和精度。 */
    valueMetadata,
    /** 来源提供的显示文本，例如“21 ℃”；计算仍使用 result 中的原始数值 21。 */
    formattedValue,
    /** 数值是否可信，例如采集来源标记“估算值”；设备在线、告警已处理属于业务状态。 */
    quality,
    /** 数值对应的采样时刻，例如设备 10:00 采到温度；10:05 入库不能把采样时间改为 10:05。 */
    observedAt,
    /** 一次事情发生的时刻，例如告警记录的 alarmTime；区别于读取告警列表的时间。 */
    occurredAt,
    /** 执行或统计区间从何时开始，例如一次通知发送的开始时刻、小时指标的窗口起点。 */
    startedAt,
    /** 执行或统计区间到何时结束；有结束时刻也不自动表示发送成功或统计数据完整。 */
    endedAt,
    /** 来源保存这条记录的时刻，例如温度补报数据的入库时间；不能代替原采样时间。 */
    recordedAt,
    /** 业务当前处于什么状态，例如告警“已处理”、通知“发送失败”；值及含义由原业务定义。 */
    status
}
