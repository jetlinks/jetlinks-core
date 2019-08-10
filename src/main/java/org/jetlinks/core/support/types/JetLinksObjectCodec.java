package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.support.JetLinksPropertyMetadata;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class JetLinksObjectCodec implements DataTypeCodec<ObjectType> {

    @Override
    public String getTypeId() {
        return ObjectType.ID;
    }

    @Override
    public ObjectType decode(ObjectType type, Map<String, Object> config) {
        JSONObject jsonObject = new JSONObject(config);

        ofNullable(jsonObject.getJSONArray("properties"))
                .map(list -> list
                        .stream()
                        .map(JSONObject.class::cast)
                        .<PropertyMetadata>map(JetLinksPropertyMetadata::new)
                        .collect(Collectors.toList()))
                .ifPresent(type::setProperties);

        return type;
    }

    @Override
    public Map<String, Object> encode(ObjectType type) {
        JSONObject json = new JSONObject();
        json.put("properties", type.getProperties()
                .stream()
                .map(PropertyMetadata::toJson)
                .collect(Collectors.toList()));

        return json;
    }
}
