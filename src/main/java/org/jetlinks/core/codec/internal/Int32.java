package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class Int32 implements Codec<Integer> {
    @Override
    public Class<Integer> forType() {
        return Integer.class;
    }

    @Override
    public String getId() {
        return "int32";
    }

    @Override
    public int byteLength() {
        return 4;
    }

    @Override
    public Integer decode(@Nonnull ByteBuf payload) {
        return payload.readInt();
    }

    @Override
    public ByteBuf encode(Integer body, ByteBuf buf) {
        return buf.writeInt(body);
    }
}
