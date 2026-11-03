package org.jetlinks.core.metadata;

import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.utils.CompositeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
    public List<ThingMetadata> getModules() {
        List<ThingMetadata> first = left.getModules();
        List<ThingMetadata> second = left == right ? first : right.getModules();
        if (first.isEmpty()) {
            return Collections.unmodifiableList(second);
        }
        if (second.isEmpty() || first == second) {
            return Collections.unmodifiableList(first);
        }
        Map<String, ThingMetadata> modules = new LinkedHashMap<>();
        first.forEach(module -> modules.put(module.getId(), module));
        second.forEach(module -> modules.putIfAbsent(module.getId(), module));
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    @Override
    public List<RelationMetadata> getRelationMetadata() {
        // 模型可变，按读取合并；使用键索引避免逐项扫描和 JSON 编码比较。
        Map<List<Object>, RelationMetadata> result = new LinkedHashMap<>();
        mergeRelations(result, left.getRelationMetadata());
        if (left != right) {
            mergeRelations(result, right.getRelationMetadata());
        }
        return Collections.unmodifiableList(new ArrayList<>(result.values()));
    }

    private void mergeRelations(Map<List<Object>, RelationMetadata> result, List<RelationMetadata> relations) {
        for (RelationMetadata relation : relations) {
            List<Object> key = java.util.Arrays.asList(relation.getTargetType(), relation.getId(), relation.isReverse());
            RelationMetadata previous = result.putIfAbsent(key, relation);
            if (previous != null && !previous.equals(relation)) {
                throw new I18nSupportException.NoStackTrace("error.conflicting_relation_definition",
                    relation.getTargetType(), relation.getId());
            }
        }
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
        // 与属性/功能的组合规则一致：主模型优先，不能让无关扩展的数量改变语义。
        Map<String, Object> expands = new LinkedHashMap<>(right.getExpands());
        expands.putAll(left.getExpands());
        return Collections.unmodifiableMap(expands);
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
