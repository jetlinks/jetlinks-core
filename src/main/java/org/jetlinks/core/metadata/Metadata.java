package org.jetlinks.core.metadata;

import java.util.Map;
import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface Metadata {

    String getId();

    String getName();

    String getDescription();

    Map<String, Object> getExpands();

    default Optional<Object> getExpand(String name) {
        return Optional.ofNullable(getExpands())
                .map(map -> map.get(name));
    }

    default void setExpands(Map<String, Object> expands) {
    }

    default void setName(String name) {

    }

    default void setDescription(String description) {

    }

}
