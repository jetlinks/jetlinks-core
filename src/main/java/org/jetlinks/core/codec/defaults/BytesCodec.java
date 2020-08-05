package org.jetlinks.core.codec.defaults;

import io.netty.buffer.Unpooled;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class BytesCodec implements Codec<byte[]> {

    public static BytesCodec INSTANCE = new BytesCodec();

    private BytesCodec() {

    }

    @Override
    public Class<byte[]> forType() {
        return byte[].class;
    }

    @Override
    public byte[] decode(@Nonnull Payload payload) {
        return payload.getBytes();
    }

    @Override
    public Payload encode(byte[] body) {
        return () -> Unpooled.wrappedBuffer(body);
    }


}
