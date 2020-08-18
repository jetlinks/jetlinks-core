package org.jetlinks.core.metadata;

import java.util.List;
import java.util.Optional;

/**
 * 物模型
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMetadata extends Metadata, Jsonable {

    List<PropertyMetadata> getProperties();

    List<FunctionMetadata> getFunctions();

    List<EventMetadata> getEvents();

    List<PropertyMetadata> getTags();

    default Optional<EventMetadata> getEvent(String id) {
        return Optional.ofNullable(getEventOrNull(id));
    }

    EventMetadata getEventOrNull(String id);

    default Optional<PropertyMetadata> getProperty(String id) {
        return Optional.ofNullable(getPropertyOrNull(id));
    }

    PropertyMetadata getPropertyOrNull(String id);

    default Optional<FunctionMetadata> getFunction(String id) {
        return Optional.ofNullable(getFunctionOrNull(id));
    }

    FunctionMetadata getFunctionOrNull(String id);

    default Optional<PropertyMetadata> getTag(String id) {
        return Optional.ofNullable(getTagOrNull(id));
    }

    PropertyMetadata getTagOrNull(String id);
}
