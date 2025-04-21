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
    public void encode(Float body, Buffer buf, ByteOrder endian) {
        int intVal = (int) (body * 10);

        int high = intVal >> 8;
        int low = intVal & 0xff;

        if (endian == ByteOrder.LITTLE_ENDIAN) {
            buf.byteBuf().writeByte(low);
            buf.byteBuf().writeByte(high);
        } else {
            buf.byteBuf().writeByte(high);
            buf.byteBuf().writeByte(low);
        }
    }

    @Override
    public Float decode(@Nonnull Buffer payload, ByteOrder endian) {
        byte high = payload.byteBuf().readByte();
        byte low = payload.byteBuf().readByte();
        //默认大端.小端时,低字节在前.
        if (endian == ByteOrder.LITTLE_ENDIAN) {
            byte tmp = low;
            low = high;
            high = tmp;
        }

        int fistValue = high << 8;
        int secondValue = low;

        return (fistValue + secondValue) / 10F;
    }
}
