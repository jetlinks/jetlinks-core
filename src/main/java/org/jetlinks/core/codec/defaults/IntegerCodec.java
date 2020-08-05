package org.jetlinks.core.codec.defaults;

import io.netty.buffer.Unpooled;
import org.jetlinks.core.utils.BytesUtils;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class IntegerCodec implements Codec<Integer> {

    public static IntegerCodec INSTANCE = new IntegerCodec();

    private IntegerCodec() {

    }

    @Override
    public Class<Integer> forType() {
        return Integer.class;
    }

    @Override
    public Integer decode(@Nonnull Payload payload) {
        return BytesUtils.beToInt(payload.getBytes());
    }

    @Override
    public Payload encode(Integer body) {
        return () -> Unpooled.wrappedBuffer(BytesUtils.intToBe(body));
    }


}
