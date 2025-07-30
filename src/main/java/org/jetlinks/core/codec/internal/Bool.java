package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class Bool implements Codec<Boolean> {
    @Override
    public Class<Boolean> forType() {
        return Boolean.class;
    }

    @Override
    public String getId() {
        return "bool";
    }

    @Override
    public int byteLength() {
        return 1;
    }

    @Override
    public Boolean decode(@Nonnull ByteBuf payload) {
        return payload.readBoolean();
    }

    @Override
    public ByteBuf encode(Boolean body, ByteBuf buf) {
        return buf.writeBoolean(body);
    }
}
