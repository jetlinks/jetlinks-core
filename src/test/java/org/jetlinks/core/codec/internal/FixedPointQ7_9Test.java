package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class FixedPointQ7_9Test {

    private final FixedPointQ7_9 codec = new FixedPointQ7_9();
    private static final float PRECISION = 1.0f / 512; // Q7.9的精度

    @Test
    public void testForType() {
        assertEquals(Float.class, codec.forType());
    }

    @Test
    public void testBasicRoundTrip() {
        // 测试基本往返转换
        testRoundTrip(0.0f);
        testRoundTrip(1.0f);
        testRoundTrip(-1.0f);
        testRoundTrip(32.5f);
        testRoundTrip(-32.5f);
        testRoundTrip(63.0f);
        testRoundTrip(-64.0f);
    }

    @Test
    public void testBoundaryValues() {
        // 测试边界值：Q7.9的范围是-64.0到63.998046875
        testRoundTrip(63.998046875f);  // 最大正值 (32767/512)
        testRoundTrip(-64.0f);         // 最小负值

        // 测试接近边界的值
        testRoundTrip(63.0f);
        testRoundTrip(-63.5f);
        testRoundTrip(60.5f);
        testRoundTrip(-60.5f);
    }

    @Test
    public void testPrecisionValues() {
        // 测试精度相关的值：Q7.9的精度是1/512

        // 可以精确表示的值
        testRoundTrip(PRECISION);        // 最小精度
        testRoundTrip(-PRECISION);       // 负最小精度
        testRoundTrip(2 * PRECISION);    // 2倍精度
        testRoundTrip(10 * PRECISION);   // 10倍精度

        // 常见的小数值
        testRoundTrip(0.125f);    // 1/8 = 64/512
        testRoundTrip(0.25f);     // 1/4 = 128/512
        testRoundTrip(0.5f);      // 1/2 = 256/512
        testRoundTrip(0.75f);     // 3/4 = 384/512
        testRoundTrip(1.5f);      // 3/2 = 768/512
    }

    @Test
    public void testIntegerValues() {
        // 测试整数值（应该精确表示）
        for (int i = -64; i <= 63; i++) {
            testRoundTrip((float) i);
        }
    }

    @Test
    public void testZeroAndSign() {
        // 测试零值和符号处理
        testRoundTrip(0.0f);
        testRoundTrip(-0.0f);

        // 测试小的正负值
        testRoundTrip(0.001953125f);  // 1/512
        testRoundTrip(-0.001953125f);
    }

    @Test
    public void testOverflow() {
        // 测试溢出处理
        ByteBuf buf = Unpooled.buffer(2);
        

        // 正向溢出
        codec.encode(100.0f, buf); // 超过最大值
        buf.readerIndex(0);
        Float decoded = codec.decode(buf);
        assertTrue("正向溢出应该被限制", decoded <= 63.998046875f);

        // 负向溢出
        buf.clear();
        codec.encode(-100.0f, buf); // 小于最小值
        buf.readerIndex(0);
        decoded = codec.decode(buf);
        assertTrue("负向溢出应该被限制", decoded >= -64.0f);
    }

    @Test
    public void testRounding() {
        // 测试舍入行为
        ByteBuf buf = Unpooled.buffer(2);
        

        // 测试不能精确表示的值
        float[] testValues = {0.1f, 0.3f, 0.7f, 1.1f, 2.3f, 10.7f};

        for (float value : testValues) {
            codec.encode(value, buf);
            buf.readerIndex(0);
            Float decoded = codec.decode(buf);

            // 验证舍入到最近的可表示值
            float expected = Math.round(value * 512) / 512.0f;
            assertEquals(String.format("舍入失败: %f -> %f (期望: %f)", value, decoded, expected),
                        expected, decoded, PRECISION / 2);

            buf.clear();
        }
    }

    @Test
    public void testBitPattern() {
        // 测试特定的位模式
        ByteBuf buf = Unpooled.buffer(2);
        

        // 编码1.0 (应该是 0x0200 = 512 in fixed point)
        codec.encode(1.0f, buf);
        assertEquals(0x02, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));

        buf.clear();

        // 编码-1.0 (应该是 0xFE00 = -512 in fixed point)
        codec.encode(-1.0f, buf);
        assertEquals((byte)0xFE, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));

        buf.clear();

        // 编码0.5 (应该是 0x0100 = 256 in fixed point)
        codec.encode(0.5f, buf);
        assertEquals(0x01, buf.getByte(0));
        assertEquals(0x00, buf.getByte(1));
    }

    @Test
    public void testEdgeCases() {
        // 测试特殊情况
        ByteBuf buf = Unpooled.buffer(2);
        

        // 测试很小的值
        testRoundTrip(Float.MIN_VALUE); // 应该舍入到0
        testRoundTrip(1e-10f);          // 应该舍入到0

        // 测试NaN和无穷大
        codec.encode(Float.NaN, buf);
        codec.encode(Float.POSITIVE_INFINITY, buf);
        codec.encode(Float.NEGATIVE_INFINITY, buf);
    }

    @Test
    public void testSequentialPrecisionValues() {
        // 测试连续的精度值
        for (int i = 0; i < 100; i++) {
            float value = i * PRECISION;
            if (value <= 63.0f) { // 确保在范围内
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
        float expected = Math.round(value * 512) / 512.0f;

        // 验证
        assertEquals(String.format("往返转换失败: %f -> %f (期望: %f)", value, decoded, expected),
                    expected, decoded, PRECISION / 2);
    }
}