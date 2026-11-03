package org.jetlinks.core.things.relation;

/**
 * 关系在规范方向上的语义归类，不构成因果证据或完整关系词汇表。
 * 线上使用字符串，未知新词由原模型保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum RelationSemantic {
    /** 稳定结构或归属，例如空间包含设备；不表示执行先后。 */
    structural,
    /** 某条记录描述的主体，例如告警发生关于设备。 */
    about,
    /** 一次活动实际使用的输入或资源，由活动指向被使用对象。 */
    used,
    /** 一次活动实际产生的结果，由活动指向结果对象。 */
    generated,
    /** 一次事件或活动触发的后续活动，不仅表示时间相邻。 */
    triggered
}
