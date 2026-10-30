package org.jetlinks.core.things.relation;

/**
 * 关系在规范方向上的语义归类，不构成因果证据或完整关系词汇表。
 * 线上使用字符串，未知新词由原模型保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum RelationSemantic {
    structural, about, used, generated, triggered
}
