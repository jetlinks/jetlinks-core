package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class FixedPointScaled10 implements Codec<Float> {

    // (high + low) /10
    @Override
    public String getId() {
        return "fix_scaled_10";
    }

    @Override
    public int byteLength() {
        return 2;
    }

    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public ByteBuf encode(Float body, ByteBuf buf) {
        int intVal = (int) (body * 10);

        int high = intVal >> 8;
        int low = intVal & 0xff;

        return buf.writeByte(high)
                  .writeByte(low);
    }

    @Override
    public Float decode(@Nonnull ByteBuf payload) {
        byte high = payload.readByte();
        byte low = payload.readByte();

        // 修正运算符优先级问题：应该是(high << 8) + low
        return ((high << 8) + (low & 0xFF)) / 10F;
    }
}
