package org.jetlinks.core.metadata;

/**
 * 功能的业务分类，不替代授权、幂等或完成状态。
 * 线上使用字符串，未知新词由原模型保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum FunctionKind {
    query, action
}
