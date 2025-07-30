package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.codec.Codec;

import javax.annotation.Nonnull;

/**
 * Q31.1 定点数格式编解码器
 * 32位数据：31位整数部分 + 1位小数部分
 * 小数部分精度：1/2 = 0.5
 * 数值范围：-1073741824.0 ~ 1073741823.5
 */
public class FixedPointQ31_1 implements Codec<Float> {

    private static final int FRACTIONAL_BITS = 1;
    private static final int SCALE = 1 << FRACTIONAL_BITS; // 2

    @Override
    public String getId() {
        return "Q31_1";
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