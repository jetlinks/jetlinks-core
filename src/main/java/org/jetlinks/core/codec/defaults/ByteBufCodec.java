package org.jetlinks.core.codec.defaults;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class ByteBufCodec implements Codec<ByteBuf> {

    public static final ByteBufCodec INSTANCE = new ByteBufCodec();

    @Override
    public Class<ByteBuf> forType() {
        return ByteBuf.class;
    }

    @Override
    public ByteBuf decode(@Nonnull Payload payload) {
        return payload.getBody();
    }

    @Override
    public Payload encode(ByteBuf body) {
        return Payload.of(body);
    }
}
