package org.jetlinks.core.codec.internal;

import org.jetlinks.core.buffer.Buffer;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class FixedPointQ8_8 implements Codec<Float> {

    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public void encode(Float body, Buffer buf) {
        int high = (int) Math.floor(body);
        int low = Math.round(((body - high) * 100));

        buf.byteBuf().writeByte((byte) high);
        buf.byteBuf().writeByte((byte) low);
    }

    @Override
    public Float decode(@Nonnull Buffer payload) {
        int high = payload.byteBuf().readByte();
        int low = payload.byteBuf().readByte();

        return high + (low == 0 ? 0.0F : (float) (low / Math.pow(10, ((int) Math.log10(low) + 1))));
    }
}
