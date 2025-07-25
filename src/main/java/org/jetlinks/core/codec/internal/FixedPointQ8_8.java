package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.buffer.Buffer;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

public class FixedPointQ8_8 implements Codec<Float> {

    @Override
    public String getId() {
        return "Q8_8";
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
        // 标准Q8.8格式：将float值转换为定点数
        int fixedPoint = Math.round(body * 256); // 256 = 2^8

        // 限制在16位有符号整数范围内
        fixedPoint = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, fixedPoint));

        // 分解为高8位（整数部分）和低8位（小数部分）
        int high = (fixedPoint >> 8) & 0xFF;
        int low = fixedPoint & 0xFF;

        return buf
            .writeByte((byte) high)
            .writeByte((byte) low);
    }

    @Override
    public Float decode(@Nonnull ByteBuf payload) {
        // 读取两个字节，注意处理符号扩展
        byte highByte = payload.readByte();
        byte lowByte = payload.readByte();

        // 将两个字节合并为16位定点数
        int fixedPoint = (highByte << 8) | (lowByte & 0xFF);

        // 转换为浮点数：小数部分精度为1/256
        return (float) fixedPoint / 256.0f;
    }
}
