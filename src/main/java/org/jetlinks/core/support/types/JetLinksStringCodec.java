package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataTypeCodec;
import org.jetlinks.core.metadata.types.StringType;

import java.util.Map;

@Getter
@Setter
public class JetLinksStringCodec implements DataTypeCodec<StringType> {

    @Override
    public String getTypeId() {
        return StringType.ID;
    }

    @Override
    public StringType decode(StringType type, Map<String, Object> config) {

        return type;
    }

    @Override
    public Map<String, Object> encode(StringType type) {
        JSONObject json = new JSONObject();

        return json;
    }
}
