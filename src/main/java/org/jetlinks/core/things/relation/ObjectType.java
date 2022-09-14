package org.jetlinks.core.things.relation;

import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 对象类型，用于定义一个对象的分类,如: 设备,用户
 *
 * @author zhouhao
 * @since 1.2
 */
public interface ObjectType extends Metadata {

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
     * @return 获取全部关系信息
     */
    default Map<String, List<Relation>> getRelations() {
        return Collections.emptyMap();
    }

    /**
     * 获取支持建立关系的对象类型,如: 设备支持和用户建立关系
     *
     * @return 对象类型
     */
    List<ObjectType> getRelatedTypes();
}
