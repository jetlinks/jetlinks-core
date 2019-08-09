package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.DataType;

public interface ArrayType extends DataType {

    @Override
    default String getId() {
        return "array";
    }

    @Override
    default String getName() {
        return "数组";
    }

    @Override
    default String getDescription() {
        return "数组类型,由多个元素组成";
    }

    DataType getElementType();

}
