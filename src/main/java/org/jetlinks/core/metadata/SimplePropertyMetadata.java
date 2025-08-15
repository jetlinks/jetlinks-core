package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.MapUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.types.DataTypes;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimplePropertyMetadata implements PropertyMetadata {

    private DataType valueType;

    private String id;

    private String name;

    private String description;

    private Map<String, Object> expands;

    public static SimplePropertyMetadata of(PropertyMetadata metadata) {
        SimplePropertyMetadata simple = new SimplePropertyMetadata();
        simple.setId(metadata.getId());
        simple.setName(metadata.getName());
        simple.setDescription(metadata.getDescription());
        simple.setExpands(metadata.getExpands());
        simple.setValueType(metadata.getValueType());
        return simple;
    }

    public static SimplePropertyMetadata of(String id, String name, DataType type) {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setId(id);
        metadata.setName(name);
        metadata.setValueType(type);
        return metadata;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, JSONObject::new, "valueType");
        json.put("valueType", valueType.toJson());
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        FastBeanCopier.copy(json, this, "valueType");
        JSONObject object = json.getJSONObject("valueType");
        if (object != null) {
            valueType = DataTypes.fromJsonNow(object);
        }
    }

    @Override
    public PropertyMetadata merge(PropertyMetadata another, MergeOption... option) {
        SimplePropertyMetadata metadata = FastBeanCopier.copy(this, SimplePropertyMetadata::new);
        if (metadata.expands == null) {
            metadata.expands = new HashMap<>();
        }
        if (MapUtils.isNotEmpty(another.getExpands())) {
            another.getExpands().forEach(metadata.expands::put);
        }
        return metadata;
    }
    @Override
    public String toString() {
        //  /* 测试 */ int name,
        return String.join("",
                           getValueType().getId(), " ", getId(), " /* ", getName(), " */ "
        );

    }
}
