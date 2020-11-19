package org.jetlinks.core.codec.defaults;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class FastJsonCodec implements Codec<JSONObject> {

    public static final FastJsonCodec INSTANCE = new FastJsonCodec();

    @Override
    public Class<JSONObject> forType() {
        return JSONObject.class;
    }

    @Override
    public JSONObject decode(@Nonnull Payload payload) {
        return JSON.parseObject(payload.bodyToString(false));
    }

    @Override
    public Payload encode(JSONObject body) {
        return Payload.of(JSON.toJSONBytes(body));
    }

}
