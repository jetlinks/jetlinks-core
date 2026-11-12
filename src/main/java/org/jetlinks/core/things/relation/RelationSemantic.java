package org.jetlinks.core.things.relation;

/**
 * 说明一条关系的大类，具体关系仍使用来源自己的标识和名称，例如“属于产品”“分配给组织”。
 * 方向按来源声明的主语到宾语理解，反向浏览不改写事实；这些分类不是完整业务关系表。
 * 声明 used、generated、triggered 需要来源有真实依据，不能根据记录时间相邻推断因果。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum RelationSemantic {
    /** 组成、层级或归属，例如空间包含设备、组织有下级组织；不表示执行先后。 */
    structural,
    /** 一条记录描述哪个事物，例如“告警发生记录 → 设备”。 */
    about,
    /** 活动实际用了什么，例如“某次指标计算 → 输入样本”；方向从活动到输入。 */
    used,
    /** 活动实际产出了什么，例如“某次指标计算 → 计算结果”；方向从活动到输出。 */
    generated,
    /** 什么触发了后续执行，例如“本次告警 → 本次通知发送”；需要原执行关联作为依据。 */
    triggered
}
