package org.jetlinks.core.codec.defaults;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class ByteCodec implements Codec<Byte> {

    public static ByteCodec INSTANCE = new ByteCodec();

    private ByteCodec() {

    }

    @Override
    public Class<Byte> forType() {
        return Byte.class;
    }

    @Override
    public Byte decode(@Nonnull Payload payload) {
        ByteBuf buf = payload.getBody();
        byte val = buf.getByte(0);
        buf.resetReaderIndex();
        return val;
    }

    @Override
    public Payload encode(Byte body) {
        return () -> Unpooled.wrappedBuffer(new byte[]{body});
    }


}
