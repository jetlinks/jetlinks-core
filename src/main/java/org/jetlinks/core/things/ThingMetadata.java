package org.jetlinks.core.things;

import org.jetlinks.core.metadata.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface ThingMetadata extends Metadata, Jsonable {

    /**
     * @return 属性定义
     */
    List<PropertyMetadata> getProperties();

    /**
     * @return 功能定义
     */
    List<FunctionMetadata> getFunctions();

    /**
     * @return 事件定义
     */
    List<EventMetadata> getEvents();

    /**
     * @return 标签定义
     */
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

    default PropertyMetadata findProperty(Predicate<PropertyMetadata> predicate) {
        return getProperties()
                .stream()
                .filter(predicate)
                .findAny()
                .orElse(null);
    }

    default <T extends ThingMetadata> ThingMetadata merge(T metadata) {
        return merge(metadata, MergeOption.DEFAULT_OPTIONS);
    }

    default <T extends ThingMetadata>  ThingMetadata merge(T metadata, MergeOption... options) {
        throw new UnsupportedOperationException("unsupported merge metadata");
    }
}
