package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class Int16 implements Codec<Short> {
    @Override
    public Class<Short> forType() {
        return Short.class;
    }

    @Override
    public String getId() {
        return "int16";
    }

    @Override
    public int byteLength() {
        return 2;
    }

    @Override
    public Short decode(@Nonnull ByteBuf payload) {
        return payload.readShort();
    }

    @Override
    public ByteBuf encode(Short body, ByteBuf buf) {
        return buf.writeShort(body);
    }
}
