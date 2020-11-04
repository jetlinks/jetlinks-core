package org.jetlinks.core.metadata;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    public ConfigScope[] getScope() {
        return this.scopes == null ? ConfigMetadata.super.getScope() : scopes;
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

    public DefaultConfigMetadata add(String property, String name, DataType type) {
        return add(property, name, null, type);
    }

    public DefaultConfigMetadata add(String property, String name, String description, DataType type) {
        return add(Property.of(property, name, description, type));
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

    }

}
