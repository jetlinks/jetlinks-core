package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class FixedPointQ1_15Test {

    private final FixedPointQ1_15 codec = new FixedPointQ1_15();
    private static final float PRECISION = 1.0f / 32768; // Q1.15的精度

    @Test
    public void testForType() {
        assertEquals(Float.class, codec.forType());
    }

    @Test
    public void testBasicRoundTrip() {
        // 测试基本往返转换
        testRoundTrip(0.0f);
        testRoundTrip(0.5f);
        testRoundTrip(-0.5f);
        testRoundTrip(0.999f);
        testRoundTrip(-0.999f);
        testRoundTrip(0.25f);
        testRoundTrip(-0.25f);
    }

    @Test
    public void testBoundaryValues() {
        // 测试边界值：Q1.15的范围是-1.0到接近1.0
        testRoundTrip(-1.0f);     // 最小值
        testRoundTrip(0.999969482421875f);  // 接近最大值 (32767/32768)

        // 测试接近边界的值
        testRoundTrip(-0.999f);
        testRoundTrip(0.999f);
    }

    @Test
    public void testHighPrecision() {
        // 测试高精度值：Q1.15有很高的小数精度

        // 测试可以精确表示的值
        testRoundTrip(1.0f / 32768);    // 最小正精度
        testRoundTrip(-1.0f / 32768);   // 最小负精度
        testRoundTrip(2.0f / 32768);    // 2倍最小精度
        testRoundTrip(100.0f / 32768);  // 100倍最小精度

        // 测试更复杂的精确值
        testRoundTrip(0.125f);          // 1/8 = 4096/32768
        testRoundTrip(0.0625f);         // 1/16 = 2048/32768
        testRoundTrip(0.03125f);        // 1/32 = 1024/32768
    }

    @Test
    public void testZeroAndSign() {
        // 测试零值和符号
        testRoundTrip(0.0f);
        testRoundTrip(-0.0f);

        // 测试最小精度值的符号
        testRoundTrip(PRECISION);
        testRoundTrip(-PRECISION);
    }

    @Test
    public void testOverflow() {
        // 测试溢出处理：超出[-1.0, 1.0)范围的值应该被限制

        ByteBuf buf = Unpooled.buffer(2);
        

        // 正向溢出
        codec.encode(2.0f, buf); // 超过最大值
        buf.readerIndex(0);
        Float decoded = codec.decode(buf);
        assertTrue("正向溢出应该被限制", decoded < 1.0f);

        // 负向溢出
        buf.clear();
        codec.encode(-2.0f, buf); // 小于最小值
        buf.readerIndex(0);
        decoded = codec.decode(buf);
        assertTrue("负向溢出应该被限制", decoded >= -1.0f);
    }

    @Test
    public void testRounding() {
        // 测试舍入行为：不能精确表示的值应该舍入到最近的可表示值

        ByteBuf buf = Unpooled.buffer(2);
        

        // 测试一个不能精确表示的值
        float input = 0.1f; // 0.1无法精确表示为1/32768的倍数
        codec.encode(input, buf);
        buf.readerIndex(0);
        Float decoded = codec.decode(buf);

        // 验证舍入到最近值
        float expectedRounded = Math.round(input * 32768) / 32768.0f;
        assertEquals("舍入不正确", expectedRounded, decoded, 0.0001f);
    }

    @Test
    public void testBitPattern() {
        // 测试特定的位模式
        ByteBuf buf = Unpooled.buffer(2);
        

        // 编码0.5 (应该是 0x4000 = 16384 in fixed point)
        codec.encode(0.5f, buf);
        assertEquals(0x40, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));

        buf.clear();

        // 编码-0.5 (应该是 0xC000 = -16384 in fixed point)
        codec.encode(-0.5f, buf);
        assertEquals((byte)0xC0, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));

        buf.clear();

        // 编码0.25 (应该是 0x2000 = 8192 in fixed point)
        codec.encode(0.25f, buf);
        assertEquals(0x20, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));
    }

    @Test
    public void testEdgeCases() {
        // 测试特殊情况
        ByteBuf buf = Unpooled.buffer(2);
        

        // 测试非常小的值（应该舍入到0或最小精度值）
        testRoundTrip(Float.MIN_VALUE);
        testRoundTrip(1e-10f);

        // 测试NaN和无穷大的处理（应该不会崩溃）
        codec.encode(Float.NaN, buf);
        codec.encode(Float.POSITIVE_INFINITY, buf);
        codec.encode(Float.NEGATIVE_INFINITY, buf);
    }

    @Test
    public void testSequentialValues() {
        // 测试连续的可表示值
        ByteBuf buf = Unpooled.buffer(2);
        

        for (int i = 0; i < 100; i++) {
            float value = i * PRECISION;
            if (value < 1.0f) { // 确保在范围内
                testRoundTrip(value);
                testRoundTrip(-value);
            }
        }
    }

    private void testRoundTrip(float value) {
        ByteBuf buf = Unpooled.buffer(2);
        

        // 编码
        codec.encode(value, buf);

        // 重置读取位置
        buf.readerIndex(0);

        // 解码
        Float decoded = codec.decode(buf);

        // 计算期望值（舍入到最近的可表示值）
        float expected = Math.round(value * 32768) / 32768.0f;

        // 验证
        assertEquals(String.format("往返转换失败: %f -> %f (期望: %f)", value, decoded, expected),
                    expected, decoded, PRECISION / 2);
    }
}