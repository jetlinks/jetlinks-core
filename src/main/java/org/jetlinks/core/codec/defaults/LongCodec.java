package org.jetlinks.core.codec.defaults;

import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.utils.BytesUtils;

import javax.annotation.Nonnull;

public class LongCodec implements Codec<Long> {

    public static LongCodec INSTANCE = new LongCodec();

    private LongCodec() {

    }

    @Override
    public Class<Long> forType() {
        return Long.class;
    }

    @Override
    public Long decode(@Nonnull Payload payload) {
        return BytesUtils.beToLong(payload.getBytes(false));
    }

    @Override
    public Payload encode(Long body) {
        return Payload.of(BytesUtils.longToBe(body));
    }


}
