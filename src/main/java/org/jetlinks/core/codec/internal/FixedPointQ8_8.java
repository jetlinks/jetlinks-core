package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.buffer.Buffer;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;
import java.nio.ByteOrder;

public class FixedPointQ8_8 implements Codec<Float> {

    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public void encode(Float body, Buffer buf, ByteOrder endian) {
        int high = (int) Math.floor(body);
        int low = Math.round(((body - high) * 100));
        if (endian == ByteOrder.LITTLE_ENDIAN) {
            buf.byteBuf().writeByte((byte) low);
            buf.byteBuf().writeByte((byte) high);
        } else {
            buf.byteBuf().writeByte((byte) high);
            buf.byteBuf().writeByte((byte) low);
        }
    }

    @Override
    public Float decode(@Nonnull Buffer payload, ByteOrder endian) {
        int high = payload.byteBuf().readByte();
        int low = payload.byteBuf().readByte();
        //默认大端.小端时,低字节在前.
        if (endian == ByteOrder.LITTLE_ENDIAN) {
            int tmp = low;
            low = high;
            high = tmp;
        }
        return high + (low == 0 ? 0.0F : (float) (low / Math.pow(10, ((int) Math.log10(low) + 1))));
    }
}
