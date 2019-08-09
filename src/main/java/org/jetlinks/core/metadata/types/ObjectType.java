package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.List;

public interface ObjectType extends DataType {

    @Override
    default String getId() {
        return "object";
    }

    @Override
    default String getName() {
        return "对象类型";
    }

    @Override
    default String getDescription() {
        return "复杂结构对象类型";
    }

    List<PropertyMetadata> getProperties();

}
