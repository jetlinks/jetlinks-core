package org.jetlinks.core.support;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.support.types.*;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.PropertyMetadata;

import java.util.Objects;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class JetLinksPropertyMetadata implements PropertyMetadata {

    private JSONObject json;

    private transient DataType dataType;

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String description;

    public JetLinksPropertyMetadata(JSONObject json) {
        fromJson(json);
    }

    public JetLinksPropertyMetadata(PropertyMetadata another) {
        this.id = another.getId();
        this.name = another.getName();
        this.description = another.getDescription();
        this.dataType = another.getValueType();
    }

    protected DataType parseDataType() {
        JSONObject dataTypeJson = json.getJSONObject("valueType");
        if (dataTypeJson == null) {
            throw new IllegalArgumentException("属性" + getId() + "类型不能为空");
        }
        DataType dataType;
        switch (dataTypeJson.getString("type")) {
            case "int":
                dataType = new IntType();
                break;
            case "string":
                dataType = new StringType();
                break;
            case "boolean":
                dataType = new BooleanType();
                break;
            case "double":
                dataType = new DoubleType();
                break;
            default:
                dataType = new UnknownType();
                break;
        }
        if (dataType instanceof Jsonable) {
            ((Jsonable) dataType).fromJson(dataTypeJson);
        }
        return dataType;
    }

    @Override
    public DataType getValueType() {
        if (dataType == null && json != null) {
            dataType = parseDataType();
        }
        return dataType;
    }


    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description);
        if (null != getValueType()) {
            if (getValueType() instanceof Jsonable) {
                json.put("valueType", ((Jsonable) getValueType()).toJson());
            }
        }
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        Objects.requireNonNull(jsonObject);
        this.json = jsonObject;
        this.id = json.getString("id");
        this.name = json.getString("name");
        this.description = json.getString("description");
        this.dataType = null;
    }

    @Override
    public String toString() {
        //  /* 测试 */ int name,
        return String.join("", new String[]{
                " /* ", getName(), " */ ", getValueType().getId(), " ", getId()
        });

    }
}
