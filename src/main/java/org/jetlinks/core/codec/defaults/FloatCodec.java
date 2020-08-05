package org.jetlinks.core.codec.defaults;

import io.netty.buffer.Unpooled;
import org.jetlinks.core.utils.BytesUtils;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class FloatCodec implements Codec<Float> {

    public static FloatCodec INSTANCE = new FloatCodec();

    private FloatCodec() {

    }

    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public Float decode(@Nonnull Payload payload) {
        return BytesUtils.beToFloat(payload.getBytes());
    }

    @Override
    public Payload encode(Float body) {
        return () -> Unpooled.wrappedBuffer(BytesUtils.floatToBe(body));
    }


}
