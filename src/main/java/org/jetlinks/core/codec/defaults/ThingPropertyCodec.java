package org.jetlinks.core.codec.defaults;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.things.ThingProperty;

import javax.annotation.Nonnull;

public class ThingPropertyCodec implements Codec<ThingProperty> {
    public static final ThingPropertyCodec INSTANCE = new ThingPropertyCodec();

    @Override
    public Class<ThingProperty> forType() {
        return ThingProperty.class;
    }

    @Override
    public ThingProperty decode(@Nonnull Payload payload) {
        JSONObject json = payload.bodyToJson(false);

        return ThingProperty.of(json.getString("property"),json.get("value"),json.getLongValue("timestamp"),json.getString("state"));
    }

    @Override
    public Payload encode(ThingProperty body) {
        return Payload.of(JSON.toJSONBytes(body));
    }
}
