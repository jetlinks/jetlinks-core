package org.jetlinks.core.things.relation;

import lombok.Getter;
import lombok.Setter;

/**
 * 对象表述信息,用于描述对象的关系链信息.
 * <pre>{@code
 *
 * ObjectSpec.parse("device1@device:manager@user")
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2
 */
@Getter
@Setter
public class ObjectSpec {

    /**
     * 对象类型
     *
     * @see ObjectType
     */
    private String objectType;

    /**
     * 对象ID
     *
     * @see RelationObject#getId()
     */
    private String objectId;

    /**
     * 关系描述
     *
     * @see RelatedObject
     */
    private RelationSpec related;

    @Override
    public String toString() {
        String expr = objectId + "@" + objectType;
        if (related != null) {
            expr = expr + ":" + related;
        }
        return expr;
    }

    /**
     * 将表达式解析为对象描述信息.
     * <p>
     * 表达式格式: <code>objectId@objectType:relation@objectType</code>
     *
     * @param expr 表达式
     * @return 对象描述
     */
    public static ObjectSpec parse(String expr) {
        String[] objects = expr.split(":", 2);
        String[] first = objects[0].split("@");
        if (first.length != 2) {
            throw new UnsupportedOperationException("unsupported expression :" + objects[0]);
        }
        ObjectSpec spec = new ObjectSpec();
        spec.objectId = first[0];
        spec.objectType = first[1];
        if (objects.length == 2) {
            spec.related = RelationSpec.parse(objects[1]);
        }
        return spec;
    }
}
