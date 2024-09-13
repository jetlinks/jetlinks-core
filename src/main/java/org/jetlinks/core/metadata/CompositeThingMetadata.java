package org.jetlinks.core.metadata;

import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.utils.CompositeList;
import org.jetlinks.core.utils.CompositeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@EqualsAndHashCode(of = {"left", "right"})
public class CompositeThingMetadata implements ThingMetadata {
    protected final ThingMetadata left;
    protected final ThingMetadata right;

    public ThingMetadata left() {
        return left;
    }

    public ThingMetadata right() {
        return right;
    }

    @Override
    public List<PropertyMetadata> getProperties() {

        return new CompositeList<>(
            left.getProperties(),
            new ArrayList<>(Collections2.filter(
                right.getProperties(),
                prop -> left.getPropertyOrNull(prop.getId()) == null))
        );
    }

    @Override
    public List<FunctionMetadata> getFunctions() {
        return new CompositeList<>(
            left.getFunctions(),
            new ArrayList<>(Collections2.filter(
                right.getFunctions(),
                prop -> left.getFunctionOrNull(prop.getId()) == null))
        );
    }

    @Override
    public List<EventMetadata> getEvents() {
        return new CompositeList<>(
            left.getEvents(),
            new ArrayList<>(Collections2.filter(
                right.getEvents(),
                prop -> left.getEventOrNull(prop.getId()) == null))
        );
    }

    @Override
    public List<PropertyMetadata> getTags() {
        return new CompositeList<>(
            left.getTags(),
            new ArrayList<>(Collections2.filter(
                right.getTags(),
                prop -> left.getTagOrNull(prop.getId()) == null))
        );
    }

    @Override
    public EventMetadata getEventOrNull(String id) {
        EventMetadata metadata = left.getEventOrNull(id);
        if (metadata != null) {
            return metadata;
        }
        return right.getEventOrNull(id);
    }

    @Override
    public PropertyMetadata getPropertyOrNull(String id) {
        PropertyMetadata metadata = left.getPropertyOrNull(id);
        if (metadata != null) {
            return metadata;
        }
        return right.getPropertyOrNull(id);
    }

    @Override
    public FunctionMetadata getFunctionOrNull(String id) {
        FunctionMetadata metadata = left.getFunctionOrNull(id);
        if (metadata != null) {
            return metadata;
        }
        return right.getFunctionOrNull(id);
    }

    @Override
    public PropertyMetadata getTagOrNull(String id) {
        PropertyMetadata metadata = left.getTagOrNull(id);
        if (metadata != null) {
            return metadata;
        }
        return right.getTagOrNull(id);
    }

    @Override
    public String getId() {
        return left.getId();
    }

    @Override
    public String getName() {
        return left.getName();
    }

    @Override
    public String getDescription() {
        return left.getDescription();
    }

    @Override
    public Map<String, Object> getExpands() {
        if (left.getExpands() == null) {
            return right.getExpands();
        }
        if (right.getExpands() == null) {
            return left.getExpands();
        }
        return new CompositeMap<>(left.getExpands(), right.getExpands());
    }

    @Override
    public <T extends ThingMetadata> CompositeThingMetadata merge(T metadata) {
        return merge(metadata, MergeOption.DEFAULT_OPTIONS);
    }

    @Override
    public <T extends ThingMetadata> CompositeThingMetadata merge(T metadata, MergeOption... options) {
        return new CompositeThingMetadata(left.merge(metadata, options), right);
    }
}
