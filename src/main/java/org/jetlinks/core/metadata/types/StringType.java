package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.DataType;

public interface StringType extends DataType {

    @Override
    default String getId() {
        return "string";
    }

    @Override
    default String getName() {
        return "字符串";
    }

    @Override
    default String getDescription() {
        return "普通文本类型";
    }

    Integer getMaxLength();

}
