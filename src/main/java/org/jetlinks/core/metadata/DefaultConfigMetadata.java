package org.jetlinks.core.metadata;

import com.google.common.collect.Maps;
import lombok.*;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.config.ConfigKeyValue;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public class DefaultConfigMetadata implements ConfigMetadata {
    private static final long serialVersionUID = 0L;

    private String name;

    private String description;

    private String document;

    private ConfigScope[] scopes;

    private List<ConfigPropertyMetadata> properties = new ArrayList<>();

    public DefaultConfigMetadata() {

    }

    public DefaultConfigMetadata(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @SneakyThrows
    public DefaultConfigMetadata(String name, String description, Resource document) {
        this.name = name;
        this.description = description;
        try (InputStream doc = document.getInputStream()) {
            this.document = StreamUtils.copyToString(doc, StandardCharsets.UTF_8);
        }
    }

    @Override
    public ConfigScope[] getScopes() {
        return this.scopes == null ? ConfigMetadata.super.getScopes() : scopes;
    }

    @Override
    public List<ConfigPropertyMetadata> getProperties() {
        return properties;
    }

    public DefaultConfigMetadata scope(ConfigScope... scopes) {
        this.scopes = scopes;
        return this;
    }

    public DefaultConfigMetadata add(ConfigPropertyMetadata metadata) {
        properties.add(metadata);
        return this;
    }

    public DefaultConfigMetadata add(String property,
                                     String name,
                                     DataType type,
                                     Consumer<Property> custom) {
        Property prop = Property.of(property, name, description, type, all, null);
        custom.accept(prop);
        return add(prop);
    }

    public DefaultConfigMetadata add(String property,
                                     String name,
                                     DataType type) {
        return add(property, name, null, type);
    }

    public DefaultConfigMetadata add(String property,
                                     String name,
                                     String description,
                                     DataType type) {
        return add(Property.of(property, name, description, type, all, null));
    }

    public DefaultConfigMetadata add(String property,
                                     String name,
                                     DataType type,
                                     ConfigScope... scopes) {
        return add(property, name, null, type, scopes);
    }

    public DefaultConfigMetadata add(String property,
                                     String name,
                                     String description,
                                     DataType type,
                                     ConfigScope... scopes) {
        return add(Property.of(property, name, description, type, scopes, null));
    }

    @Override
    public DefaultConfigMetadata copy(ConfigScope... scopes) {
        DefaultConfigMetadata configMetadata = new DefaultConfigMetadata(name, description);
        configMetadata.scopes = this.scopes;
        configMetadata.document = this.document;
        if (scopes == null || scopes.length == 0) {
            configMetadata.properties = new ArrayList<>(properties);
        } else {
            configMetadata.properties =
                this.properties
                    .stream()
                    .filter(conf -> conf.hasAnyScope(scopes))
                    .collect(Collectors.toList());
        }

        return configMetadata;
    }

    @Override
    public ConfigMetadata merge(ConfigMetadata another) {
        DefaultConfigMetadata configMetadata = new DefaultConfigMetadata(name, description);

        ConfigScope[] scope = Stream
            .concat(Stream.of(getScopes()), Stream.of(another.getScopes()))
            .toArray(ConfigScope[]::new);

        if (scope.length == 0) {
            scope = all;
        }
        configMetadata.scopes = scope;
        configMetadata.document = this.document;
        String anotherDocument;

        if (another instanceof DefaultConfigMetadata
            && StringUtils.hasText((anotherDocument = ((DefaultConfigMetadata) another).document))) {
            if (configMetadata.document == null) {
                configMetadata.document = anotherDocument;
            } else {
                configMetadata.document += "\n" + anotherDocument;
            }
        }

        configMetadata.properties = Stream
            .concat(
                this.getProperties().stream(),
                another.getProperties().stream()
            ).collect(Collectors.toList());


        return configMetadata;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(staticName = "of")
    public static class Property implements ConfigPropertyMetadata {
        private static final long serialVersionUID = 0L;

        private String property;

        private String name;

        private String description;

        private DataType type;

        private ConfigScope[] scopes;

        private Map<String, Object> expands;

        public Property description(String description) {
            this.description = description;
            return this;
        }

        public Property scope(ConfigScope... scopes) {
            this.scopes = scopes;
            return this;
        }

        public Property expands(Map<String, Object> expands) {
            if (CollectionUtils.isEmpty(expands)) {
                return this;
            }
            if (this.expands == null) {
                this.expands = new HashMap<>();
            } else if (!(this.expands instanceof HashMap)) {
                this.expands = Maps.newHashMap(this.expands);
            }
            this.expands.putAll(expands);
            return this;
        }

        public Property expand(ConfigKeyValue<?>... kvs) {
            for (ConfigKeyValue<?> kv : kvs) {
                expand(kv.getKey(), kv.getValue());
            }
            return this;
        }

        public <V> Property expand(ConfigKey<V> configKey, V value) {
            return expand(configKey.getKey(), value);
        }

        public Property expand(String configKey, Object value) {
            if (value == null) {
                return this;
            }
            if (expands == null) {
                expands = new HashMap<>();
            } else if (!(this.expands instanceof HashMap)) {
                this.expands = Maps.newHashMap(this.expands);
            }
            expands.put(configKey, value);
            return this;
        }

    }

}
