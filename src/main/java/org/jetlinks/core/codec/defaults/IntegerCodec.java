package org.jetlinks.core.codec.defaults;

import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.utils.BytesUtils;

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
        return BytesUtils.beToInt(payload.getBytes(false));
    }

    @Override
    public Payload encode(Integer body) {
        return Payload.of(BytesUtils.intToBe(body));
    }


}
