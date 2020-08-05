package org.jetlinks.core.codec.defaults;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class FastJsonArrayCodec implements Codec<JSONArray> {

    public static final FastJsonArrayCodec INSTANCE=new FastJsonArrayCodec();

    @Override
    public Class<JSONArray> forType() {
        return JSONArray.class;
    }

    @Override
    public JSONArray decode(@Nonnull Payload payload) {
        return JSON.parseArray(payload.bodyToString());
    }

    @Override
    public Payload encode(JSONArray body) {
        return () -> Unpooled.wrappedBuffer(JSON.toJSONBytes(body));
    }

}
