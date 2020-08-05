package org.jetlinks.core.codec.defaults;

import io.netty.buffer.Unpooled;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class BooleanCodec implements Codec<Boolean> {

    public static BooleanCodec INSTANCE = new BooleanCodec();

    private BooleanCodec() {

    }

    @Override
    public Class<Boolean> forType() {
        return Boolean.class;
    }

    @Override
    public Boolean decode(@Nonnull Payload payload) {
        byte[] data = payload.getBytes();

        return data.length > 0 && data[0] > 0;
    }

    @Override
    public Payload encode(Boolean body) {
        return () -> Unpooled.wrappedBuffer(new byte[]{body ? (byte) 1 : 0});
    }

}
