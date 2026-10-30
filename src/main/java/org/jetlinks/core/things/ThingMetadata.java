package org.jetlinks.core.things;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.metadata.*;
import org.jetlinks.core.things.relation.Relation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    /**
     * 可选关系目录，键为另一端对象类型。仅包含声明，不加载目标模型或关系实例。
     * 缺省为空；旧来源不能据此被认为已支持任意关系读取。
     *
     * @return 非空关系目录
     * @since 1.3.3
     */
    default Map<String, List<Relation>> getRelations() {
        return Collections.emptyMap();
    }

    /**
     * @param type 另一端对象类型
     * @return 适用关系定义，反向投影仍表示原规范关系
     * @since 1.3.3
     */
    default List<Relation> getRelations(String type) {
        return getRelations().getOrDefault(type, Collections.emptyList());
    }

    /**
     * 预留功能,模块的物模型信息.
     *
     * @return 模块物模型信息
     * @since 1.2.2
     */
    default List<ThingMetadata> getModules() {
        return Collections.emptyList();
    }

    default ThingMetadata getModuleOrNull(String moduleId) {
        return getModule(moduleId).orElse(null);
    }

    default Optional<ThingMetadata> getModule(String moduleId) {
        return getModules()
            .stream()
            .filter(metadata -> Objects.equals(moduleId, metadata.getId()))
            .findAny();
    }

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

    default <T extends ThingMetadata> ThingMetadata merge(T metadata, MergeOption... options) {
        throw new UnsupportedOperationException("unsupported merge metadata");
    }

    @Override
    default ThingMetadata expand(String key, Object value) {
        Metadata.super.expand(key, value);
        return this;
    }

    @Override
    default <T> ThingMetadata expand(ConfigKey<T> key, T value) {
        Metadata.super.expand(key, value);
        return this;
    }

    @Override
    default JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", getId());
        json.put("name", getName());
        json.put("description", getDescription());
        json.put("properties", getProperties().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("functions", getFunctions().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("events", getEvents().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("tags", getTags().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("expands", getExpands());
        // 空目录保持旧模型形状；关系只编码定义，不递归展开相关类型。
        if (!getRelations().isEmpty()) {
            JSONObject relations = new JSONObject(true);
            getRelations().forEach((type, values) -> relations.put(type,
                values.stream().map(Relation::toJson).collect(Collectors.toList())));
            json.put("relations", relations);
        }
        if (!getModules().isEmpty()) {
            json.put("modules", getModules().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        }
        return json;
    }
}
