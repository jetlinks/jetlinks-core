package org.jetlinks.core.metadata;

import org.jetlinks.core.config.ConfigKey;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface PropertyMetadata extends Metadata, Jsonable {

    DataType getValueType();

    default PropertyMetadata merge(PropertyMetadata another, MergeOption... option) {
        throw new UnsupportedOperationException("不支持属性物模型合并");
    }

    @Override
    default PropertyMetadata expand(String key, Object value) {
        Metadata.super.expand(key, value);
        return this;
    }

    @Override
    default <T> Metadata expand(ConfigKey<T> key, T value) {
        Metadata.super.expand(key, value);
        return this;
    }
}
