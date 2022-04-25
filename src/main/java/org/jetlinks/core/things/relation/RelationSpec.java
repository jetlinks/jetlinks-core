package org.jetlinks.core.things.relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;

import java.util.HashMap;
import java.util.Map;

/**
 * 关系描述
 *
 * @author zhouhao
 * @since 1.2
 */
@Getter
@Setter
public class RelationSpec {

    public static final String OPTION_REVERSE = "reverse";

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
     * 其他选项
     */
    private Map<String, Object> options;

    /**
     * 下一级关系
     *
     * @see RelationSpec
     */
    private RelationSpec next;

    @JsonIgnore
    public boolean isReverse() {
        return options != null && Boolean.TRUE.equals(options.get(OPTION_REVERSE));
    }

    public void reverse(boolean reverse) {
        option(OPTION_REVERSE, reverse);
    }

    @Override
    public String toString() {
        String rel = relation;
        if (options != null) {
            rel = rel + "$" + String.join("$", options.keySet());
        }
        String expr = rel + "@" + objectType;
        if (next != null) {
            expr = expr + ":" + next;
        }
        return expr;
    }

    public RelationSpec option(String key, Object value) {
        if (options == null) {
            options = new HashMap<>();
        }
        options.put(key, value);
        return this;
    }

    public static RelationSpec of(Object spec){
        if(spec instanceof RelationSpec){
            return ((RelationSpec) spec);
        }
        if(spec instanceof String){
            return parse(String.valueOf(spec));
        }
        return FastBeanCopier.copy(spec,new RelationSpec());
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
        String relation = first[0];
        if (relation.contains("$")) {
            String[] rel = relation.split("\\$", 2);
            relation = rel[0];
            for (int i = 1; i < rel.length; i++) {
                String opt = rel[i];
                spec.option(opt, true);
            }
        }
        spec.relation = relation;
        spec.objectType = first[1];
        if (objects.length == 2) {
            spec.next = RelationSpec.parse(objects[1]);
        }
        return spec;
    }
}
