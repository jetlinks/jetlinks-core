package org.jetlinks.core.config;

public interface ConfigKey<V> {
    String getKey();

    default String getName() {
        return getKey();
    }

    default Class<V> getType() {
        return (Class<V>) Object.class;
    }

    static <T> ConfigKey<T> of(String key) {
        return () -> key;
    }

    default ConfigKeyValue<V> value(V value) {
        return new ConfigKeyValue<V>() {
            @Override
            public V getValue() {
                return value;
            }

            @Override
            public String getKey() {
                return ConfigKey.this.getKey();
            }

            @Override
            public String getName() {
                return ConfigKey.this.getName();
            }

            @Override
            public Class<V> getType() {
                return ConfigKey.this.getType();
            }
        };
    }
}
