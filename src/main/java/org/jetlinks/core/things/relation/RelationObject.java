package org.jetlinks.core.things.relation;

/**
 * 关系对象(RelationObject): 某一类型对应的具体的数据,如: 具体的某个用户,设备等
 *
 * @author zhouhao
 * @since 1.2
 */
public interface RelationObject {

    /**
     * 成员ID
     *
     * @return 成员ID
     */
    String getId();

    /**
     * 成员类型
     *
     * @return 类型
     * @see ObjectType#getId()
     */
    String getType();

    /**
     * 开始对成员进行关系操作.
     * <p>
     * 如果参数{@code reverse}为{@code false},则为正向关系,比如: 设备的管理员；为{@code true}时,表示反向关系,如: 管理的设备
     *
     * @param reverse 是否反转关系
     * @return 关系操作接口
     */
    RelationOperation relations(boolean reverse);

    /**
     * 开始对成员属性进行操作
     *
     * @return 属性操作接口
     */
    PropertyOperation properties();

}
