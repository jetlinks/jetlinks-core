package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class FixedPointQ1_31Test {

    private final FixedPointQ1_31 codec = new FixedPointQ1_31();
    private static final float PRECISION = 1.0f / 2147483648L; // Q1_31的精度

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
        testRoundTrip(0.999999f);
        testRoundTrip(-0.999999f);
        testRoundTrip(0.25f);
        testRoundTrip(-0.25f);
    }

    @Test
    public void testBoundaryValues() {
        // 测试边界值：Q1.31的范围是-1.0到接近1.0
        testRoundTrip(-1.0f);     // 最小值
        testRoundTrip(0.9999999995343387f);  // 接近最大值

        // 测试接近边界的值
        testRoundTrip(-0.999999f);
        testRoundTrip(0.999999f);
    }

    @Test
    public void testUltraHighPrecision() {
        // 测试超高精度值：Q1.31有极高的小数精度

        // 测试可以精确表示的值
        testRoundTrip(PRECISION);           // 最小正精度
        testRoundTrip(-PRECISION);          // 最小负精度
        testRoundTrip(2 * PRECISION);       // 2倍最小精度
        testRoundTrip(1000 * PRECISION);    // 1000倍最小精度

        // 测试常见小数的高精度表示
        testRoundTrip(0.125f);      // 1/8
        testRoundTrip(0.0625f);     // 1/16
        testRoundTrip(0.03125f);    // 1/32
        testRoundTrip(0.015625f);   // 1/64
        testRoundTrip(0.0078125f);  // 1/128
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

        ByteBuf buf = Unpooled.buffer(4);
        

        // 正向溢出
        codec.encode(2.0f, buf); // 超过最大值
        buf.readerIndex(0);
        Float decoded = codec.decode(buf);
        assertTrue("正向溢出应该被限制", decoded <= 1.0f);

        // 负向溢出
        buf.clear();
        codec.encode(-2.0f, buf); // 小于最小值
        buf.readerIndex(0);
        decoded = codec.decode(buf);
        assertTrue("负向溢出应该被限制", decoded >= -1.0f);
    }

    @Test
    public void testHighPrecisionRounding() {
        // 测试高精度舍入行为

        ByteBuf buf = Unpooled.buffer(4);
        

        // 测试一些不能精确表示的值
        float[] testValues = {0.1f, 0.3f, 0.7f, 0.9f};

        for (float value : testValues) {
            codec.encode(value, buf);
            buf.readerIndex(0);
            Float decoded = codec.decode(buf);

            // 验证舍入到最近的可表示值
            long fixedValue = Math.round(value * 2147483648L);
            float expected = (float) fixedValue / 2147483648L;

            assertEquals(String.format("高精度舍入失败: %f -> %f (期望: %f)", value, decoded, expected),
                        expected, decoded, PRECISION);

            buf.clear();
        }
    }

    @Test
    public void testBitPattern() {
        // 测试特定的位模式
        ByteBuf buf = Unpooled.buffer(4);
        

        // 编码0.5 (应该是 0x40000000 = 1073741824 in fixed point)
        codec.encode(0.5f, buf);
        assertEquals(0x40, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));
        assertEquals(0x00, buf.getByte(2));
        assertEquals(0x00, buf.getByte(3));

        buf.clear();

        // 编码-0.5 (应该是 0xC0000000 = -1073741824 in fixed point)
        codec.encode(-0.5f, buf);
        assertEquals((byte)0xC0, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));
        assertEquals(0x00, buf.getByte(2));
        assertEquals(0x00, buf.getByte(3));

        buf.clear();

        // 编码0.25 (应该是 0x20000000 = 536870912 in fixed point)
        codec.encode(0.25f, buf);
        assertEquals(0x20, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));
        assertEquals(0x00, buf.getByte(2));
        assertEquals(0x00, buf.getByte(3));
    }

    @Test
    public void testEdgeCases() {
        // 测试特殊情况
        ByteBuf buf = Unpooled.buffer(4);
        

        // 测试非常小的值（应该能表示或舍入到最小精度值）
        testRoundTrip(Float.MIN_VALUE);
        testRoundTrip(1e-10f);

        // 测试NaN和无穷大的处理（应该不会崩溃）
        codec.encode(Float.NaN, buf);
        codec.encode(Float.POSITIVE_INFINITY, buf);
        codec.encode(Float.NEGATIVE_INFINITY, buf);
    }

    @Test
    public void testSequentialMicroprecisionValues() {
        // 测试连续的微精度值
        for (int i = 0; i < 1000; i++) {
            double value = i * PRECISION;
            if (value < 1.0) { // 确保在范围内
                testRoundTrip((float) value);
                testRoundTrip((float) -value);
            }
        }
    }

    @Test
    public void testFractionalBinaryValues() {
        // 测试二进制分数值（这些应该能精确表示）
        float[] binaryFractions = {
            1.0f/2,    // 0.5
            1.0f/4,    // 0.25
            1.0f/8,    // 0.125
            1.0f/16,   // 0.0625
            1.0f/32,   // 0.03125
            1.0f/64,   // 0.015625
            1.0f/128,  // 0.0078125
            1.0f/256,  // 0.00390625
            1.0f/512,  // 0.001953125
            1.0f/1024  // 0.0009765625
        };

        for (float fraction : binaryFractions) {
            testRoundTrip(fraction);
            testRoundTrip(-fraction);

            // 测试组合值
            testRoundTrip(0.5f + fraction);
            testRoundTrip(-0.5f - fraction);
        }
    }

    private void testRoundTrip(float value) {
        ByteBuf buf = Unpooled.buffer(4);
        

        // 编码
        codec.encode(value, buf);

        // 重置读取位置
        buf.readerIndex(0);

        // 解码
        Float decoded = codec.decode(buf);

        // 计算期望值（舍入到最近的可表示值）
        long fixedValue = Math.round(value * 2147483648L);
        float expected = (float) fixedValue / 2147483648L;

        // 验证
        assertEquals(String.format("往返转换失败: %f -> %f (期望: %f)", value, decoded, expected),
                    expected, decoded, PRECISION);
    }
}