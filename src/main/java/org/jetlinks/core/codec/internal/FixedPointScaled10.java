package org.jetlinks.core.codec.internal;

import org.jetlinks.core.buffer.Buffer;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;
import java.nio.ByteOrder;

public class FixedPointScaled10 implements Codec<Float> {

    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public void encode(Float body, Buffer buf) {
        int intVal = (int) (body * 10);

        int high = intVal >> 8;
        int low = intVal & 0xff;

        buf.byteBuf().writeByte(high);
        buf.byteBuf().writeByte(low);
    }

    @Override
    public Float decode(@Nonnull Buffer payload) {
        byte high = payload.byteBuf().readByte();
        byte low = payload.byteBuf().readByte();

        return (high << 8 + low) / 10F;
    }
}
