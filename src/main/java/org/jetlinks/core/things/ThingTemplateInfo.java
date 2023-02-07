package org.jetlinks.core.things;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.config.ConfigKeyValue;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
public class ThingTemplateInfo {
    private String type;

    private String id;

    private String name;

    private String metadata;

    private Long version;

    private Map<String, Object> configuration;

    public ThingTemplateInfo addConfig(String key, Object value) {
        if (configuration == null) {
            configuration = new HashMap<>();
        }
        configuration.put(key, value);
        return this;
    }

    public ThingTemplateInfo addConfigIfAbsent(String key, Object value) {
        if (configuration == null) {
            configuration = new HashMap<>();
        }
        configuration.putIfAbsent(key, value);
        return this;
    }

    public ThingTemplateInfo addConfigs(Map<String, ?> configs) {
        if (configs == null) {
            return this;
        }
        configs.forEach(this::addConfig);
        return this;
    }

    public <T> ThingTemplateInfo addConfig(ConfigKey<T> key, T value) {
        addConfig(key.getKey(), value);
        return this;
    }

    public <T> ThingTemplateInfo addConfig(ConfigKeyValue<T> keyValue) {
        addConfig(keyValue.getKey(), keyValue.getValue());
        return this;
    }
}
