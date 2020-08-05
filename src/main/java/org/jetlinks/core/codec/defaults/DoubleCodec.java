package org.jetlinks.core.codec.defaults;

import io.netty.buffer.Unpooled;
import org.jetlinks.core.Payload;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.utils.BytesUtils;

import javax.annotation.Nonnull;

public class DoubleCodec implements Codec<Double> {

    public static DoubleCodec INSTANCE = new DoubleCodec();

    private DoubleCodec() {

    }

    @Override
    public Class<Double> forType() {
        return Double.class;
    }

    @Override
    public Double decode(@Nonnull Payload payload) {
        return BytesUtils.beToDouble(payload.getBytes());
    }

    @Override
    public Payload encode(Double body) {
        return () -> Unpooled.wrappedBuffer(BytesUtils.doubleToBe(body));
    }


}
