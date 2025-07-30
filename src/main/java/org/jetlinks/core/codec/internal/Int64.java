package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class Int64 implements Codec<Long> {
    @Override
    public Class<Long> forType() {
        return Long.class;
    }

    @Override
    public String getId() {
        return "int64";
    }

    @Override
    public int byteLength() {
        return 8;
    }

    @Override
    public Long decode(@Nonnull ByteBuf payload) {
        return payload.readLong();
    }

    @Override
    public ByteBuf encode(Long body, ByteBuf buf) {
        return buf.writeLong(body);
    }
}
