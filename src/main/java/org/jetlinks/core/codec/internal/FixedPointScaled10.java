package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class FixedPointScaled10 implements Codec<Float> {

    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public void encode(Float body, ByteBuf buf, Endian endian) {
        int intVal = (int) (body * 10);

        int high = intVal >> 8;
        int low = intVal & 0xff;

        if (endian == Endian.Little) {
            buf.writeByte(low);
            buf.writeByte(high);
        } else {
            buf.writeByte(high);
            buf.writeByte(low);
        }
    }

    @Override
    public Float decode(@Nonnull ByteBuf payload, Endian endian) {
        byte high = payload.readByte();
        byte low = payload.readByte();
        //默认大端.小端时,低字节在前.
        if (endian == Endian.Little) {
            byte tmp = low;
            low = high;
            high = tmp;
        }

        int fistValue = high << 8;
        int secondValue = low;

        return (fistValue + secondValue) / 10F;
    }
}
