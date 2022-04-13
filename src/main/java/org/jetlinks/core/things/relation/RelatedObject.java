package org.jetlinks.core.things.relation;

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
