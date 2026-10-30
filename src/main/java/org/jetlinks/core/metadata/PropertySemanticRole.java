package org.jetlinks.core.metadata;

/**
 * 字段在来源记录中的角色；字段标识、单位和值类型仍由原模型定义。
 * 线上使用字符串，未知新词由原模型保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum PropertySemanticRole {
    identity, observedProperty, result, quality,
    observedAt, occurredAt, startedAt, endedAt, recordedAt, status
}
