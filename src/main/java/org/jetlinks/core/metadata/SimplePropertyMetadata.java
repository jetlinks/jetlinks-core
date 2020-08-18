package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SimplePropertyMetadata implements PropertyMetadata {

    private DataType valueType;

    private String id;

    private String name;

    private String description;

    private Map<String, Object> expands;


    @Override
    public void fromJson(JSONObject json) {
        throw new UnsupportedOperationException();
    }


}
