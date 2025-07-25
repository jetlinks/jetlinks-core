package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.buffer.Buffer;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

/**
 * Q1.31 定点数格式编解码器
 * 32位数据：1位整数部分 + 31位小数部分
 * 小数部分精度：1/2147483648 ≈ 4.656612873e-10
 * 数值范围：-1.0 ~ 0.9999999995343387127
 */
public class FixedPointQ1_31 implements Codec<Float> {

    private static final int FRACTIONAL_BITS = 31;
    private static final long SCALE = 1L << FRACTIONAL_BITS; // 2147483648

    @Override
    public String getId() {
        return "Q1_31";
    }

    @Override
    public int byteLength() {
        return 4;
    }

    @Override
    public Class<Float> forType() {
        return Float.class;
    }

    @Override
    public ByteBuf encode(Float body, ByteBuf buf) {
        // 写入32位数据（大端序）
        return buf.writeInt(Math.round(body * SCALE));
    }

    @Override
    public Float decode(@Nonnull ByteBuf payload) {
        // 读取32位有符号整数
        int fixedPoint = payload.readInt();

        // 转换为浮点数
        return (float) fixedPoint / SCALE;
    }
}