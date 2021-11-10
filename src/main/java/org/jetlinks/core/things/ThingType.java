package org.jetlinks.core.things;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * 物类型定义,通常使用枚举实现此接口
 *
 * @author zhouhao
 * @see TopicSupport
 * @since 1.1.9
 */
public interface ThingType extends TopicSupport {
    /**
     * @return 类型ID
     */
    String getId();

    /**
     * @return 类型名称
     */
    String getName();

    /**
     * @return 是否为内置的类型定义
     */
    default boolean isEmbedded() {
        return this instanceof Enum;
    }

    default boolean isSameType(ThingType thingType) {
        return this == thingType
                || Objects.equals(thingType.getId(), this.getId());
    }

    @Override
    default String getTopicPrefix(String templateId, String thingId) {
        StringJoiner joiner = new StringJoiner("/", "/", "");
        joiner.add(getId()).add(templateId).add(thingId);
        return joiner.toString();
    }

    /**
     * 根据id创建ThingType,如果ThingType未定义,则创建新的ThingType
     *
     * @param id typeId
     * @return ThingType
     */
    static ThingType of(String id) {
        return ThingTypes.lookupOrElse(id, UndefinedThingType::of);
    }
}
