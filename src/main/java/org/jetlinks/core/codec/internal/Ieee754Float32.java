package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class Ieee754Float32 implements Codec<Float> {
    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public String getId() {
        return "ieee754_float32";
    }

    @Override
    public int byteLength() {
        return 4;
    }

    @Override
    public Float decode(@Nonnull ByteBuf payload) {
        return payload.readFloat();
    }

    @Override
    public ByteBuf encode(Float body, ByteBuf buf) {
        return buf.writeFloat(body);
    }
}
