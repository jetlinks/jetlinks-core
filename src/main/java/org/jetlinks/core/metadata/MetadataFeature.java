package org.jetlinks.core.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MetadataFeature implements Feature {
    supportDerivedMetadata("设备支持派生物模型"),

    propertyNotModifiable("物模型属性不可修改"),
    propertyTypeNotModifiable("物模型属性数据类型不可修改"),
    propertyNotDeletable("物模型属性不可删除"),
    propertyNotInsertable("物模型属性不可新增"),

    functionNotInsertable("物模型功能不可新增"),
    functionNotModifiable("物模型功能不可修改"),
    functionNotDeletable("物模型功能不可删除"),

    eventNotInsertable("物模型事件不可新增"),
    eventNotModifiable("物模型事件不可修改"),
    eventNotDeletable("物模型事件不可删除"),

    ;

    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
