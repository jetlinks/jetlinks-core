package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.buffer.Buffer;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

/**
 * Q15.1 定点数格式编解码器
 * 16位数据：15位整数部分 + 1位小数部分
 * 小数部分精度：1/2 = 0.5
 * 数值范围：-16384.0 ~ 16383.5
 */
public class FixedPointQ15_1 implements Codec<Float> {

    private static final int FRACTIONAL_BITS = 1;
    private static final int SCALE = 1 << FRACTIONAL_BITS; // 2

    @Override
    public String getId() {
        return "Q15_1";
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
        // 将float值转换为定点数格式
        int fixedPoint = Math.round(body * SCALE);

        // 限制在16位有符号整数范围内
        fixedPoint = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, fixedPoint));

        // 写入16位数据（大端序）
        return buf.writeShort(fixedPoint);
    }

    @Override
    public Float decode(@Nonnull ByteBuf payload) {
        // 读取16位有符号整数
        short fixedPoint = payload.readShort();

        // 转换为浮点数
        return (float) fixedPoint / SCALE;
    }
}