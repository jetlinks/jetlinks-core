package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.types.DoubleType;

import java.util.Map;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class JetLinksDoubleCodec implements DataTypeCodec<DoubleType> {

    @Override
    public String getTypeId() {
        return DoubleType.ID;
    }

    @Override
    public DoubleType decode(DoubleType type, Map<String, Object> config) {
        JSONObject jsonObject = new JSONObject(config);
        ofNullable(jsonObject.getDouble("max"))
                .ifPresent(type::setMax);
        ofNullable(jsonObject.getDouble("min"))
                .ifPresent(type::setMin);
        ofNullable(jsonObject.getInteger("scale"))
                .ifPresent(type::setScale);
        ofNullable(jsonObject.get("unit"))
                .map(JetLinksStandardValueUnit::of)
                .ifPresent(type::setUnit);
        return type;
    }

    @Override
    public Map<String, Object> encode(DoubleType type) {
        JSONObject json = new JSONObject();
        json.put("max", type.getMax());
        json.put("min", type.getMin());

        json.put("scale", type.getScale());
        if (type.getUnit() != null) {
            json.put("unit", type.getUnit().getId());
        }
        return json;
    }
}
