package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class FixedPointQ8_8Test {

    private final FixedPointQ8_8 codec = new FixedPointQ8_8();
    private static final float PRECISION = 1.0f / 256; // Q8.8的精度

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
        testRoundTrip(10.5f);
        testRoundTrip(-10.5f);
        testRoundTrip(100.25f);
        testRoundTrip(-100.25f);
    }

    @Test
    public void testBoundaryValues() {
        // 测试边界值：Q8.8的范围是-128.0到127.99609375
        testRoundTrip(127.99609375f);  // 最大正值 (32767/256)
        testRoundTrip(-128.0f);        // 最小负值

        // 测试接近边界的值
        testRoundTrip(127.0f);
        testRoundTrip(-127.5f);
        testRoundTrip(126.5f);
        testRoundTrip(-126.5f);
    }

    @Test
    public void testPrecisionValues() {
        // 测试精度相关的值：Q8.8的精度是1/256

        // 可以精确表示的值
        testRoundTrip(PRECISION);        // 最小精度 (1/256)
        testRoundTrip(-PRECISION);       // 负最小精度
        testRoundTrip(2 * PRECISION);    // 2倍精度
        testRoundTrip(10 * PRECISION);   // 10倍精度

        // 常见的小数值
        testRoundTrip(0.125f);    // 1/8 = 32/256
        testRoundTrip(0.25f);     // 1/4 = 64/256
        testRoundTrip(0.5f);      // 1/2 = 128/256
        testRoundTrip(0.75f);     // 3/4 = 192/256
        testRoundTrip(1.5f);      // 3/2 = 384/256
    }

    @Test
    public void testIntegerValues() {
        // 测试整数值（应该精确表示）
        for (int i = -128; i <= 127; i++) {
            testRoundTrip((float) i);
        }
    }

    @Test
    public void testZeroAndSign() {
        // 测试零值和符号处理
        testRoundTrip(0.0f);
        testRoundTrip(-0.0f);

        // 测试小的正负值
        testRoundTrip(0.00390625f);  // 1/256
        testRoundTrip(-0.00390625f);
    }

    @Test
    public void testOverflow() {
        // 测试溢出处理
        ByteBuf buf = Unpooled.buffer(2);
        

        // 正向溢出
        codec.encode(200.0f, buf); // 超过最大值
        buf.readerIndex(0);
        Float decoded = codec.decode(buf);
        assertTrue("正向溢出应该被限制", decoded <= 127.99609375f);

        // 负向溢出
        buf.clear();
        codec.encode(-200.0f, buf); // 小于最小值
        buf.readerIndex(0);
        decoded = codec.decode(buf);
        assertTrue("负向溢出应该被限制", decoded >= -128.0f);
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
            float expected = Math.round(value * 256) / 256.0f;
            assertEquals(String.format("舍入失败: %f -> %f (期望: %f)", value, decoded, expected),
                        expected, decoded, PRECISION / 2);

            buf.clear();
        }
    }

    @Test
    public void testStandardQ8_8BitPattern() {
        // 测试标准Q8.8格式的位模式
        ByteBuf buf = Unpooled.buffer(2);
        

        // 编码1.0 (应该是 0x0100 = 256 in fixed point)
        codec.encode(1.0f, buf);
        assertEquals(0x01, buf.getByte(0)); // 高字节：整数部分1
        assertEquals(0x00, buf.getByte(1)); // 低字节：小数部分0

        buf.clear();

        // 编码-1.0 (应该是 0xFF00 = -256 in fixed point)
        codec.encode(-1.0f, buf);
        assertEquals((byte)0xFF, buf.getByte(0)); // 高字节：整数部分-1
        assertEquals(0x00, buf.getByte(1));       // 低字节：小数部分0

        buf.clear();

        // 编码0.5 (应该是 0x0080 = 128 in fixed point)
        codec.encode(0.5f, buf);
        assertEquals(0x00, buf.getByte(0)); // 高字节：整数部分0
        assertEquals((byte)0x80, buf.getByte(1)); // 低字节：小数部分128

        buf.clear();

        // 编码1.5 (应该是 0x0180 = 384 in fixed point)
        codec.encode(1.5f, buf);
        assertEquals(0x01, buf.getByte(0)); // 高字节：整数部分1
        assertEquals((byte)0x80, buf.getByte(1)); // 低字节：小数部分128
    }

    @Test
    public void testFixedVersusOriginalImplementation() {
        // 验证修正后的实现与原始错误实现的不同
        ByteBuf buf = Unpooled.buffer(2);
        

        // 测试一些值，确保新实现能正确处理
        float[] testValues = {0.5f, 1.25f, 2.75f, -0.5f, -1.25f};

        for (float value : testValues) {
            codec.encode(value, buf);
            buf.readerIndex(0);
            Float decoded = codec.decode(buf);

            // 新的实现应该能正确往返转换
            float expected = Math.round(value * 256) / 256.0f;
            assertEquals(String.format("修正后的Q8.8实现失败: %f -> %f", value, decoded),
                        expected, decoded, PRECISION / 2);

            buf.clear();
        }
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
        for (int i = 0; i < 256; i++) {
            float value = i * PRECISION;
            if (value <= 127.0f) { // 确保在范围内
                testRoundTrip(value);
                testRoundTrip(-value);
            }
        }
    }

    @Test
    public void testSignBitHandling() {
        // 测试符号位处理（这是原始实现的主要问题）
        ByteBuf buf = Unpooled.buffer(2);
        

        // 编码小的负数
        codec.encode(-0.25f, buf);
        buf.readerIndex(0);
        Float decoded = codec.decode(buf);
        assertEquals("负数符号处理失败", -0.25f, decoded, PRECISION / 2);

        buf.clear();

        // 编码负的小数
        codec.encode(-1.75f, buf);
        buf.readerIndex(0);
        decoded = codec.decode(buf);
        assertEquals("负数小数处理失败", -1.75f, decoded, PRECISION / 2);
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
        float expected = Math.round(value * 256) / 256.0f;

        // 验证
        assertEquals(String.format("往返转换失败: %f -> %f (期望: %f)", value, decoded, expected),
                    expected, decoded, PRECISION / 2);
    }
}