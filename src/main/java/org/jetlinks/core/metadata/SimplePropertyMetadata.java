package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public static SimplePropertyMetadata of(String id, String name, DataType type) {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setId(id);
        metadata.setName(name);
        metadata.setValueType(type);
        return metadata;
    }

    @Override
    public void fromJson(JSONObject json) {
        throw new UnsupportedOperationException();
    }


}
