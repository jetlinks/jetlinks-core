package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.types.DateTimeType;

import java.util.Map;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class JetLinksDateCodec implements DataTypeCodec<DateTimeType> {

    @Override
    public String getTypeId() {
        return DateTimeType.ID;
    }

    @Override
    public DateTimeType decode(DateTimeType type, Map<String, Object> config) {
        JSONObject jsonObject = new JSONObject(config);
        ofNullable(jsonObject.getString("format"))
                .ifPresent(type::setFormat);
        ofNullable(jsonObject.getString("tz"))
                .ifPresent(type::setTzOffset);

        return type;
    }

    @Override
    public Map<String, Object> encode(DateTimeType type) {
        JSONObject json = new JSONObject();
        json.put("format", type.getFormat());
        json.put("tz", type.getTzOffset());

        return json;
    }
}
