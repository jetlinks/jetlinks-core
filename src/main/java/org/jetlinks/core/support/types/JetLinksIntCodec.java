package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.types.IntType;

import java.util.Map;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class JetLinksIntCodec implements DataTypeCodec<IntType> {

    @Override
    public String getTypeId() {
        return IntType.ID;
    }

    @Override
    public IntType decode(IntType type, Map<String, Object> config) {
        JSONObject jsonObject = new JSONObject(config);
        ofNullable(jsonObject.getInteger("max"))
                .ifPresent(type::setMax);
        ofNullable(jsonObject.getInteger("min"))
                .ifPresent(type::setMin);
        ofNullable(jsonObject.get("unit"))
                .map(JetLinksStandardValueUnit::of)
                .ifPresent(type::setUnit);

        return type;
    }

    @Override
    public Map<String, Object> encode(IntType type) {
        JSONObject json = new JSONObject();
        json.put("max", type.getMax());
        json.put("min", type.getMin());

        if (type.getUnit() != null) {
            json.put("unit", type.getUnit().getId());
        }
        return json;
    }
}
