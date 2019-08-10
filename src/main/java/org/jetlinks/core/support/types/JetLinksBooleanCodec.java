package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.types.BooleanType;

import java.util.Map;

import static java.util.Optional.ofNullable;


@Getter
@Setter
public class JetLinksBooleanCodec implements DataTypeCodec<BooleanType> {

    @Override
    public String getTypeId() {
        return BooleanType.ID;
    }

    @Override
    public BooleanType decode(BooleanType type, Map<String, Object> config) {
        JSONObject jsonObject = new JSONObject(config);

        ofNullable(jsonObject.getString("trueText"))
                .ifPresent(type::setTrueText);
        ofNullable(jsonObject.getString("falseText"))
                .ifPresent(type::setFalseText);
        ofNullable(jsonObject.getString("trueValue"))
                .ifPresent(type::setTrueValue);
        ofNullable(jsonObject.getString("falseValue"))
                .ifPresent(type::setFalseValue);

        return type;
    }

    @Override
    public Map<String, Object> encode(BooleanType type) {
        JSONObject json = new JSONObject();
        json.put("trueText", type.getTrueText());
        json.put("falseText", type.getFalseText());
        json.put("trueValue", type.getTrueValue());
        json.put("falseValue", type.getFalseValue());

        return json;
    }
}
