package org.jetlinks.core.codec.internal.arrays;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

/**
 * 将字节数组按位解析为无符号 1 位整数数组, 每个字节的 8 位从低位到高位(LSB first)依次转换.
 */
public class LSBUInt1Array implements Codec<Integer[]> {

    @Override
    public Class<Integer[]> forType() {
        return Integer[].class;
    }

    @Override
    public String getId() {
        return "lsb_uint1_array";
    }

    @Override
    public int byteLength() {
        return -1;
    }

    @Override
    public Integer[] decode(@Nonnull ByteBuf payload) {
        int size = payload.readableBytes();
        Integer[] result = new Integer[size * 8];
        for (int i = 0; i < size; i++) {
            byte b = payload.readByte();
            int offset = i * 8;
            result[offset] = (b & 0x01) != 0 ? 1 : 0;
            result[offset + 1] = (b & 0x02) != 0 ? 1 : 0;
            result[offset + 2] = (b & 0x04) != 0 ? 1 : 0;
            result[offset + 3] = (b & 0x08) != 0 ? 1 : 0;
            result[offset + 4] = (b & 0x10) != 0 ? 1 : 0;
            result[offset + 5] = (b & 0x20) != 0 ? 1 : 0;
            result[offset + 6] = (b & 0x40) != 0 ? 1 : 0;
            result[offset + 7] = (b & 0x80) != 0 ? 1 : 0;
        }
        return result;
    }

    @Override
    public ByteBuf encode(Integer[] body, ByteBuf buf) {
        if (body == null || body.length == 0) {
            return buf;
        }

        int size = body.length;
        int fullBytes = size / 8;

        for (int i = 0; i < fullBytes; i++) {
            int offset = i * 8;
            int b = 0;
            if (isOne(body[offset])) b |= 0x01;
            if (isOne(body[offset + 1])) b |= 0x02;
            if (isOne(body[offset + 2])) b |= 0x04;
            if (isOne(body[offset + 3])) b |= 0x08;
            if (isOne(body[offset + 4])) b |= 0x10;
            if (isOne(body[offset + 5])) b |= 0x20;
            if (isOne(body[offset + 6])) b |= 0x40;
            if (isOne(body[offset + 7])) b |= 0x80;
            buf.writeByte(b);
        }

        int remaining = size % 8;
        if (remaining > 0) {
            int offset = fullBytes * 8;
            int b = 0;
            for (int i = 0; i < remaining; i++) {
                if (isOne(body[offset + i])) {
                    b |= (1 << i);
                }
            }
            buf.writeByte(b);
        }

        return buf;
    }

    private boolean isOne(Integer value) {
        return value != null && value != 0;
    }
}
