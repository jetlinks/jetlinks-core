package org.jetlinks.core.codec.internal.arrays;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

/**
 * 转换字节为位数组.
 * 将字节数组的每一位转换为布尔值数组，每个字节的8位从高位到低位（MSB first）依次转换.
 * 例如：字节 0b10100000 将转换为 [true, false, true, false, false, false, false, false]
 *
 * @author zhouhao
 * @since 1.2
 */
public class BitArray implements Codec<Boolean[]> {

    @Override
    public Class<Boolean[]> forType() {
        return Boolean[].class;
    }

    @Override
    public String getId() {
        return "bit_array";
    }

    @Override
    public int byteLength() {
        return -1;
    }

    /**
     * Merges non‑null bits from source into target
     */
    public static Boolean[] mergeBits(Boolean[] source, Boolean[] target) {
        if (source == null || target == null) {
            return target;
        }

        int length = Math.min(source.length, target.length);
        for (int i = 0; i < length; i++) {
            if (source[i] != null) {
                target[i] = source[i];
            }
        }

        return target;
    }

    /**
     * 合并位数字到 ByteBuf
     *
     * @param bits   位数字
     * @param origin 原始 ByteBuf
     * @return 合并后的 ByteBuf
     */
    public static ByteBuf mergeBits(Boolean[] bits, ByteBuf origin) {
        if (bits == null || bits.length == 0) {
            return origin;
        }

        int size = bits.length;
        int fullBytes = size / 8;

        // Writes full bytes representing bit array
        for (int i = 0; i < fullBytes; i++) {
            int offset = i * 8;
            int b = 0;
            if (Boolean.TRUE.equals(bits[offset])) b |= 0x80;
            if (Boolean.TRUE.equals(bits[offset + 1])) b |= 0x40;
            if (Boolean.TRUE.equals(bits[offset + 2])) b |= 0x20;
            if (Boolean.TRUE.equals(bits[offset + 3])) b |= 0x10;
            if (Boolean.TRUE.equals(bits[offset + 4])) b |= 0x08;
            if (Boolean.TRUE.equals(bits[offset + 5])) b |= 0x04;
            if (Boolean.TRUE.equals(bits[offset + 6])) b |= 0x02;
            if (Boolean.TRUE.equals(bits[offset + 7])) b |= 0x01;
            origin.writeByte(b);
        }

        int remaining = size % 8;
        if (remaining > 0) {
            int offset = fullBytes * 8;
            int b = 0;
            for (int i = 0; i < remaining; i++) {
                if (Boolean.TRUE.equals(bits[offset + i])) {
                    b |= (1 << (7 - i));
                }
            }
            origin.writeByte(b);
        }

        return origin;
    }

    /**
     * 解码：将字节数组的每一位转换为布尔值数组
     * 每个字节的8位从高位到低位（MSB first）依次转换
     *
     * @param payload ByteBuf
     * @return 布尔值数组，每个元素代表一个位（true表示1，false表示0）
     */
    @Override
    public Boolean[] decode(@Nonnull ByteBuf payload) {
        int size = payload.readableBytes();
        Boolean[] result = new Boolean[size * 8];
        for (int i = 0; i < size; i++) {
            byte b = payload.readByte();
            int offset = i * 8;
            result[offset] = (b & 0x80) != 0;
            result[offset + 1] = (b & 0x40) != 0;
            result[offset + 2] = (b & 0x20) != 0;
            result[offset + 3] = (b & 0x10) != 0;
            result[offset + 4] = (b & 0x08) != 0;
            result[offset + 5] = (b & 0x04) != 0;
            result[offset + 6] = (b & 0x02) != 0;
            result[offset + 7] = (b & 0x01) != 0;
        }

        return result;
    }

    /**
     * 编码：将布尔值数组转换为字节数组
     * 每8个布尔值组成一个字节，从高位到低位（MSB first）排列
     * 如果布尔值数组的长度不是8的倍数，最后一个字节的剩余位将填充为0
     *
     * @param body 布尔值数组
     * @param buf  ByteBuf
     * @return ByteBuf
     */
    @Override
    public ByteBuf encode(Boolean[] body, ByteBuf buf) {
        return mergeBits(body, buf);
    }
}
