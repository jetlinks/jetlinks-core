package org.jetlinks.core;

public interface JsonViews {

    /** 简要视图，例如列表页、下拉框、基础信息 */
    interface Simple {}

    /** 完整视图，例如详情页 */
    interface Detail extends Simple {}

    /** 创建时需要的字段 */
    interface Create {}

    /** 更新时需要的字段 */
    interface Update extends Create {}

}
