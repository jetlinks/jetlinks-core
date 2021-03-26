package org.jetlinks.core.metadata;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface PropertyMetadata extends Metadata, Jsonable {

    DataType getValueType();

    default PropertyMetadata merge(PropertyMetadata another, MergeOption... option){
        throw new UnsupportedOperationException("不支持属性物模型合并");
    }
}
