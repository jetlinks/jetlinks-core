package org.jetlinks.core.metadata;

/**
 * 说明功能是读取信息还是请求业务执行，例如查设备温度与下发设备指令。
 * 此分类用于理解用途，不代替来源授权、重试规则或执行结果判断；自定义词保留在原模型中。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
public enum FunctionKind {
    /** 读取数据，例如查询设备属性快照；是否有权读取仍由原查询控制。 */
    query,
    /** 请求执行或修改状态，例如下发设备指令；请求受理不代表设备已执行成功。 */
    action
}
