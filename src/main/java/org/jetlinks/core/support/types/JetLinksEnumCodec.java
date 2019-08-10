package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.types.EnumType;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class JetLinksEnumCodec implements DataTypeCodec<EnumType> {

    @Override
    public String getTypeId() {
        return EnumType.ID;
    }

    @Override
    public EnumType decode(EnumType type, Map<String, Object> config) {
        JSONObject jsonObject = new JSONObject(config);


        ofNullable(jsonObject.getJSONArray("enums"))
                .map(list -> list.stream()
                        .map(JSONObject.class::cast)
                        .map(e -> EnumType.Element.of(e.getString("value"), e.getString("text")))
                        .collect(Collectors.toList()))
                .ifPresent(type::setElements);

        return type;
    }

    @Override
    public Map<String, Object> encode(EnumType type) {
        JSONObject json = new JSONObject();
        json.put("enums", type.getElements()
                .stream()
                .map(EnumType.Element::toMap).collect(Collectors.toList()));

        return json;
    }
}
