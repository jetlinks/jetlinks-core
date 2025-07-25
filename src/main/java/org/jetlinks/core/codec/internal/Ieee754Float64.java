package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class Ieee754Float64 implements Codec<Double> {
    @Override
    public Class<Double> forType() {
        return Double.class;
    }

    @Override
    public String getId() {
        return "ieee754_float32";
    }

    @Override
    public int byteLength() {
        return 8;
    }

    @Override
    public Double decode(@Nonnull ByteBuf payload) {
        return payload.readDouble();
    }

    @Override
    public ByteBuf encode(Double body, ByteBuf buf) {
        return buf.writeDouble(body);
    }
}
