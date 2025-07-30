package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class Int8 implements Codec<Byte> {
    @Override
    public Class<Byte> forType() {
        return Byte.class;
    }

    @Override
    public String getId() {
        return "int8";
    }

    @Override
    public int byteLength() {
        return 1;
    }

    @Override
    public Byte decode(@Nonnull ByteBuf payload) {
        return payload.readByte();
    }

    @Override
    public ByteBuf encode(Byte body, ByteBuf buf) {
        return buf.writeByte(body);
    }
}
