package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.types.LongType;

import java.util.Map;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class JetLinksLongCodec implements DataTypeCodec<LongType> {

    @Override
    public String getTypeId() {
        return LongType.ID;
    }

    @Override
    public LongType decode(LongType type, Map<String, Object> config) {
        JSONObject jsonObject = new JSONObject(config);
        ofNullable(jsonObject.getLong("max"))
                .ifPresent(type::setMax);
        ofNullable(jsonObject.getLong("min"))
                .ifPresent(type::setMin);
        ofNullable(jsonObject.get("unit"))
                .map(JetLinksStandardValueUnit::of)
                .ifPresent(type::setUnit);
        return type;
    }

    @Override
    public Map<String, Object> encode(LongType type) {
        JSONObject json = new JSONObject();
        json.put("max", type.getMax());
        json.put("min", type.getMin());
        if (type.getUnit() != null) {
            json.put("unit", type.getUnit().getId());
        }
        return json;
    }
}
