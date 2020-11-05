package org.jetlinks.core.metadata;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class DefaultConfigMetadata implements ConfigMetadata {
    private static final long serialVersionUID = 0L;

    private String name;

    private String description;

    private ConfigScope[] scopes;

    public DefaultConfigMetadata() {

    }

    public DefaultConfigMetadata(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public ConfigScope[] getScopes() {
        return this.scopes == null ? ConfigMetadata.super.getScopes() : scopes;
    }

    private List<ConfigPropertyMetadata> properties = new ArrayList<>();

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
                                     DataType type) {
        return add(property, name, null, type);
    }

    public DefaultConfigMetadata add(String property,
                                     String name,
                                     String description,
                                     DataType type) {
        return add(Property.of(property, name, description, type, all));
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
        return add(Property.of(property, name, description, type, scopes));
    }

    @Override
    public ConfigMetadata copy(ConfigScope... scopes) {
        DefaultConfigMetadata configMetadata = new DefaultConfigMetadata(name, description);
        configMetadata.scopes = this.scopes;
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

    }

}
