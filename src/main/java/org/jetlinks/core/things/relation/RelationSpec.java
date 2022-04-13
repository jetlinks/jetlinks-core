package org.jetlinks.core.things.relation;

import lombok.Getter;
import lombok.Setter;

/**
 * 关系描述
 *
 * @author zhouhao
 * @since 1.2
 */
@Getter
@Setter
public class RelationSpec {

    /**
     * 对象类型
     *
     * @see ObjectType
     */
    private String objectType;

    /**
     * 关系
     *
     * @see Relation#getId()
     */
    private String relation;

    /**
     * 下一级关系
     *
     * @see RelationSpec
     */
    private RelationSpec next;

    @Override
    public String toString() {
        String expr = relation + "@" + objectType;
        if (next != null) {
            expr = expr + ":" + next;
        }
        return expr;
    }

    /**
     * 根据表达式解析关系描述,表达式格式: <code>relation@objectType:relation2@objectType</code>
     *
     * @param expr 表达式
     * @return 关系描述
     */
    public static RelationSpec parse(String expr) {
        String[] objects = expr.split(":", 2);
        String[] first = objects[0].split("@");
        if (first.length != 2) {
            throw new UnsupportedOperationException("unsupported expression :" + objects[0]);
        }
        RelationSpec spec = new RelationSpec();
        spec.relation = first[0];
        spec.objectType = first[1];
        if (objects.length == 2) {
            spec.next = RelationSpec.parse(objects[1]);
        }
        return spec;
    }
}
