package org.jetlinks.core.metadata;


import org.jetlinks.core.config.ConfigKey;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface EventMetadata extends Metadata, Jsonable {

    DataType getType();

    default DataType getValueType() {
        return getType();
    }

    default EventMetadata merge(EventMetadata another, MergeOption... option) {
        throw new UnsupportedOperationException("不支持事件物模型合并");
    }

    @Override
    default <T> EventMetadata expand(ConfigKey<T> key, T value) {
        Metadata.super.expand(key, value);
        return this;
    }

    @Override
    default Metadata expand(String key, Object value) {
        Metadata.super.expand(key, value);
        return this;
    }
}
