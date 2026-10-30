package org.jetlinks.core.things.relation;

import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.RelationMetadata;
import org.jetlinks.core.things.ThingMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

/**
 * 对象类型，用于定义一个对象的分类,如: 设备,用户
 *
 * @author zhouhao
 * @since 1.2
 */
public interface ObjectType extends ThingMetadata {

    /** 旧类型未声明功能，不能据默认空目录推断已支持动态执行。 @since 1.3.3 */
    @Override
    default List<FunctionMetadata> getFunctions() {
        return Collections.emptyList();
    }

    /** @return 已声明事件，旧类型缺省为空 @since 1.3.3 */
    @Override
    default List<EventMetadata> getEvents() {
        return Collections.emptyList();
    }

    /** @return 已声明标签，旧类型缺省为空 @since 1.3.3 */
    @Override
    default List<PropertyMetadata> getTags() {
        return Collections.emptyList();
    }

    @Override
    default PropertyMetadata getPropertyOrNull(String id) {
        List<PropertyMetadata> properties = getProperties();
        return properties == null ? null : properties.stream()
            .filter(property -> Objects.equals(id, property.getId())).findFirst().orElse(null);
    }

    @Override
    default FunctionMetadata getFunctionOrNull(String id) {
        return getFunctions().stream().filter(function -> Objects.equals(id, function.getId())).findFirst().orElse(null);
    }

    @Override
    default EventMetadata getEventOrNull(String id) {
        return getEvents().stream().filter(event -> Objects.equals(id, event.getId())).findFirst().orElse(null);
    }

    @Override
    default PropertyMetadata getTagOrNull(String id) {
        return getTags().stream().filter(tag -> Objects.equals(id, tag.getId())).findFirst().orElse(null);
    }

    /**
     * 获取对象的属性模型信息
     *
     * @return 属性模型
     */
    List<PropertyMetadata> getProperties();

    /**
     * 获取和另外一个对象类型的关系定义,如: 获取设备和用户之间存在的关系定义
     *
     * @param type 对象类型
     * @return 关系定义
     */
    List<Relation> getRelations(String type);

    /**
     * 获取是否与另外一个类型的对象成立某种关系
     *
     * @param type 对象类型
     * @return 关系定义
     */
    default boolean hasRelation(String type, String relation) {
        List<Relation> rels = getRelations(type);
        if (rels.isEmpty()) {
            return false;
        }
        for (Relation re : rels) {
            if (re.getId().equals(relation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 获取全部关系信息
     */
    default Map<String, List<Relation>> getRelations() {
        return Collections.emptyMap();
    }

    /**
     * 复用已发布的关系 SPI 生成模型目录；保留原 Map 接口供关系管理运行时使用。
     * @return 含目标类型的关系描述
     * @since 1.3.3
     */
    @Override
    default List<RelationMetadata> getRelationMetadata() {
        List<RelationMetadata> result = new ArrayList<>();
        getRelations().forEach((type, relations) -> relations.forEach(relation ->
            result.add(new RelationMetadata(type, relation))));
        return result;
    }

    /**
     * 获取支持建立关系的对象类型,如: 设备支持和用户建立关系
     *
     * @return 对象类型
     */
    List<ObjectType> getRelatedTypes();
}
