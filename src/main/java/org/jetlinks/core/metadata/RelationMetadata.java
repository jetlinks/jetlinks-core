package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.things.relation.Relation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 关系定义的可传输描述，包含另一端类型，不加载对象实例或目标物模型。
 * reverse 表示规范关系的反向投影，不改变关系 ID 和规范语义。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.things.ThingMetadata#getRelationMetadata()
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class RelationMetadata implements Metadata, Relation, Jsonable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String targetType;
    private String name;
    private String description;
    private String reverseName;
    private boolean reverse;
    private Map<String, Object> expands;

    /**
     * 从已有关系声明取得独立的目录项；扩展值仍遵循原元数据的值语义。
     * @param targetType 另一端类型
     * @param relation 原关系声明
     */
    public RelationMetadata(String targetType, Relation relation) {
        this.targetType = required(targetType, "targetType");
        this.id = required(relation.getId(), "id");
        this.name = relation.getName();
        this.reverseName = relation.getReverseName();
        this.reverse = relation.isReverse();
        this.expands = relation.getExpands() == null ? null : new LinkedHashMap<>(relation.getExpands());
        if (relation instanceof Metadata) {
            this.description = ((Metadata) relation).getDescription();
        }
    }

    @Override
    public void fromJson(JSONObject json) {
        id = required(json.getString("id"), "id");
        targetType = required(json.getString("targetType"), "targetType");
        name = json.getString("name");
        description = json.getString("description");
        reverseName = json.getString("reverseName");
        reverse = json.getBooleanValue("reverse");
        expands = json.getJSONObject("expands");
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", required(id, "id"));
        json.put("targetType", required(targetType, "targetType"));
        json.put("name", name);
        json.put("description", description);
        json.put("reverseName", reverseName);
        json.put("reverse", reverse);
        json.put("expands", expands);
        return json;
    }

    private static String required(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new I18nSupportException.NoStackTrace("error.invalid_relation_definition", field);
        }
        return value;
    }
}
