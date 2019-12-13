package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.config.ConfigKeyValue;
import org.jetlinks.core.metadata.DataType;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@SuppressWarnings("all")
public abstract class AbstractType<R> implements DataType {

    private Map<String, Object> expands;

    private String description;

    public R expands(Map<String, Object> expands) {
        if (this.expands == null) {
            this.expands = new HashMap<>();
        }
        return (R) this;
    }

    public <V> R expand(ConfigKeyValue<V>... kvs) {
        for (ConfigKeyValue<V> kv : kvs) {
            expand(kv, kv.getValue());
        }
        return (R) this;
    }

    public <V> R expand(ConfigKey<V> configKey, V value) {
        return expand(configKey.getKey(), value);
    }

    public R expand(String configKey, Object value) {

        if (value == null) {
            return (R) this;
        }
        if (expands == null) {
            expands = new HashMap<>();
        }
        expands.put(configKey, value);
        return (R) this;
    }


}
