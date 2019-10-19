package org.jetlinks.core.config;

public interface ConfigKeyValue<V> extends ConfigKey<V> {
    V getValue();

    default boolean isNull() {
        return null == getValue();
    }

    default boolean isNotNull() {
        return null != getValue();
    }

}
