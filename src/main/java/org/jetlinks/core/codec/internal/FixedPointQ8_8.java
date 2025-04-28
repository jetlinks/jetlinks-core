package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class FixedPointQ8_8 implements Codec<Float> {

    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public void encode(Float body, ByteBuf buf, Endian endian) {
        int high = (int) Math.floor(body);
        int low = Math.round(((body - high) * 100));
        if (endian == Endian.Little) {
            buf.writeByte((byte) low);
            buf.writeByte((byte) high);
        } else {
            buf.writeByte((byte) high);
            buf.writeByte((byte) low);
        }
    }

    @Override
    public Float decode(@Nonnull ByteBuf payload, Endian endian) {
        int high = payload.readByte();
        int low = payload.readByte();
        //默认大端.小端时,低字节在前.
        if (endian == Endian.Little) {
            int tmp = low;
            low = high;
            high = tmp;
        }
        return high + (low == 0 ? 0.0F : (float) (low / Math.pow(10, ((int) Math.log10(low) + 1))));
    }
}
