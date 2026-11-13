package org.jetlinks.core.metadata;

import java.io.Serializable;

/**
 * 说明原字段指向哪个对象，例如设备的 productId 指向产品，productName 是来源提供的名称快照。
 * 字段值仍是原字符串或数组；本声明不创建关系、不查询对象，也不授予目标访问权。
 *
 * @param targetType 目标对象类型，不是模型或产品实例 ID
 * @param relationId 已声明关系的 ID；null 表示仅有对象引用，不据此生成关系事实
 * @param reverse 相对当前对象是否反向浏览该关系，不改变规范关系方向
 * @param labelProperty 同层已公开的名称字段；null 表示没有来源名称快照
 * @since 1.3.3
 * @see org.jetlinks.core.annotation.PropertyReference
 * @see MetadataSemantics#reference(PropertyMetadata)
 */
public record PropertyReferenceMapping(String targetType, String relationId, boolean reverse,
                                       String labelProperty) implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 单值和数组共用此映射；是否多值由原字段 DataType 决定。 */
    public PropertyReferenceMapping(String targetType, String relationId) {
        this(targetType, relationId, false, null);
    }
}
