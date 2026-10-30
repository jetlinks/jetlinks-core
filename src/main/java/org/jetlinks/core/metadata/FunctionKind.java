package org.jetlinks.core.metadata;

/**
 * 功能的业务分类，不替代授权、幂等或完成状态。
 * 线上使用字符串，未知新词由原模型保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum FunctionKind {
    /** 只读取数据或状态，不代表调用方已经获准读取。 */
    query,
    /** 请求业务执行或状态变化，受理不代表效果已完成。 */
    action
}
