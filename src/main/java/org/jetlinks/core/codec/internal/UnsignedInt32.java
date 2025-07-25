package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class UnsignedInt32 implements Codec<Number> {
    @Override
    public Class<Number> forType() {
        return Number.class;
    }

    @Override
    public String getId() {
        return "unsigned_int32";
    }

    @Override
    public int byteLength() {
        return 4;
    }

    @Override
    public Number decode(@Nonnull ByteBuf payload) {
        return payload.readUnsignedInt();
    }

    @Override
    public ByteBuf encode(Number body, ByteBuf buf) {
        return buf.writeInt(body.intValue());
    }
}
