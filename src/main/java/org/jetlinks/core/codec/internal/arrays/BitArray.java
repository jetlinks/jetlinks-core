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
     * 解码：将字节数组的每一位转换为布尔值数组
     * 每个字节的8位从高位到低位（MSB first）依次转换
     *
     * @param payload ByteBuf
     * @return 布尔值数组，每个元素代表一个位（true表示1，false表示0）
     */
    @Override
    public Boolean[] decode(@Nonnull ByteBuf payload) {
        int size = payload.readableBytes();
        int bitCount = size * 8;
        Boolean[] result = new Boolean[bitCount];
        int index = 0;

        // 遍历每个字节
        for (int i = 0; i < size; i++) {
            byte b = payload.readByte();
            // 从高位到低位（MSB first）提取每一位
            for (int bit = 7; bit >= 0; bit--) {
                result[index++] = (b & (1 << bit)) != 0;
            }
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
        if (body == null || body.length == 0) {
            return buf;
        }

        int size = body.length;
        int byteCount = (size + 7) / 8; // 向上取整，计算需要的字节数

        // 遍历每8个布尔值，组成一个字节
        for (int i = 0; i < byteCount; i++) {
            byte b = 0;
            int startIndex = i * 8;
            // 从高位到低位（MSB first）设置每一位
            for (int bit = 7; bit >= 0; bit--) {
                int index = startIndex + (7 - bit);
                if (index < size) {
                    Boolean value = body[index];
                    // 支持 Boolean 类型，null 视为 false
                    if (value != null && value) {
                        b |= (byte) (1 << bit);
                    }
                }
            }
            buf.writeByte(b);
        }

        return buf;
    }
}
