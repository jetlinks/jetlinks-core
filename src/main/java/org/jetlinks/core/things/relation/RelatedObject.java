package org.jetlinks.core.things.relation;

/**
 * 与某个对象建立了关系的对象
 */
public interface RelatedObject extends RelationObject {

    /**
     * @return 关系
     * @see Relation#getId()
     */
    String getRelation();

    /**
     * 目标关系对象ID
     *
     * @return 对象ID
     */
    String getRelatedToId();

    /**
     * 目标关系对象类型
     *
     * @return 关系类型
     */
    String getRelatedToType();

}
