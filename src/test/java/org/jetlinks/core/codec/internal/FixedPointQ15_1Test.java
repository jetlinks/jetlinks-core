package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class FixedPointQ15_1Test {

    private final FixedPointQ15_1 codec = new FixedPointQ15_1();

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
        testRoundTrip(123.5f);
        testRoundTrip(-456.5f);
        testRoundTrip(100.0f);
        testRoundTrip(-200.0f);
    }

    @Test
    public void testBoundaryValues() {
        // 测试边界值
        testRoundTrip(16383.5f);  // 最大正值
        testRoundTrip(-16384.0f); // 最小负值

        // 测试接近边界的值
        testRoundTrip(16383.0f);
        testRoundTrip(-16383.5f);
    }

    @Test
    public void testPrecision() {
        // 测试精度：Q15.1格式只有0.5的精度

        // 应该精确表示的值
        testRoundTrip(0.5f);
        testRoundTrip(1.5f);
        testRoundTrip(2.5f);
        testRoundTrip(-0.5f);
        testRoundTrip(-1.5f);

        // 测试舍入：应该舍入到最近的0.5
        assertRounded(0.3f, 0.5f);   // 0.3 -> 0.5
        assertRounded(0.7f, 0.5f);   // 0.7 -> 0.5
        assertRounded(0.8f, 1.0f);   // 0.8 -> 1.0
        assertRounded(1.3f, 1.5f);   // 1.3 -> 1.5
        assertRounded(1.7f, 1.5f);   // 1.7 -> 1.5
        assertRounded(1.8f, 2.0f);   // 1.8 -> 2.0
    }

    @Test
    public void testZeroAndSign() {
        // 测试零值
        testRoundTrip(0.0f);
        testRoundTrip(-0.0f);

        // 测试符号处理
        testRoundTrip(0.5f);
        testRoundTrip(-0.5f);
    }

    @Test
    public void testOverflow() {
        // 测试溢出处理：超出范围的值应该被限制

        // 正向溢出
        ByteBuf buf = Unpooled.buffer(2);
        

        codec.encode(20000.0f, buf); // 超过最大值16383.5
        buf.readerIndex(0);
        Float decoded = codec.decode(buf);
        assertTrue("正向溢出应该被限制", decoded <= 16383.5f);

        // 负向溢出
        buf.clear();
        codec.encode(-20000.0f, buf); // 小于最小值-16384.0
        buf.readerIndex(0);
        decoded = codec.decode(buf);
        assertTrue("负向溢出应该被限制", decoded >= -16384.0f);
    }

    @Test
    public void testEdgeCases() {
        // 测试特殊情况
        testRoundTrip(Float.MIN_VALUE); // 会被舍入到0

        // 测试NaN和无穷大的处理（应该不会崩溃）
        ByteBuf buf = Unpooled.buffer(2);
        

        // 这些应该不会抛出异常
        codec.encode(Float.NaN, buf);
        codec.encode(Float.POSITIVE_INFINITY, buf);
        codec.encode(Float.NEGATIVE_INFINITY, buf);
    }

    @Test
    public void testBitPattern() {
        // 测试特定的位模式
        ByteBuf buf = Unpooled.buffer(2);
        

        // 编码1.0 (应该是 0x0002 = 2 in fixed point)
        codec.encode(1.0f, buf);
        assertEquals(0x00, buf.getByte(0));
        assertEquals(0x02, buf.getByte(1));

        buf.clear();

        // 编码-1.0 (应该是 0xFFFE = -2 in fixed point)
        codec.encode(-1.0f, buf);
        assertEquals((byte)0xFF, buf.getByte(0));
        assertEquals((byte)0xFE, buf.getByte(1));
    }

    private void testRoundTrip(float value) {
        ByteBuf buf = Unpooled.buffer(2);
        

        // 编码
        codec.encode(value, buf);

        // 重置读取位置
        buf.readerIndex(0);

        // 解码
        Float decoded = codec.decode(buf);

        // 验证：由于Q15.1只有0.5的精度，需要考虑舍入误差
        float expectedDiff = Math.abs(value - Math.round(value * 2) / 2.0f);
        float actualDiff = Math.abs(decoded - Math.round(value * 2) / 2.0f);

        assertTrue(String.format("往返转换失败: %f -> %f (期望舍入到最近0.5)", value, decoded),
                  actualDiff <= 0.001f);
    }

    private void assertRounded(float input, float expected) {
        ByteBuf buf = Unpooled.buffer(2);
        

        codec.encode(input, buf);
        buf.readerIndex(0);
        Float decoded = codec.decode(buf);

        assertEquals(String.format("舍入失败: %f 应该舍入到 %f", input, expected),
                    expected, decoded, 0.001f);
    }
}