package org.jetlinks.core.metadata;

/**
 * 模型所描述数据的语义角色，可组合使用；不表示互斥的业务类型。
 * 线上使用字符串，未知新词由原模型保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum SemanticType {
    resource, observation, event, activity, result
}
