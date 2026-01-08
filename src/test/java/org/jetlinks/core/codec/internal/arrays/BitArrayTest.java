package org.jetlinks.core.codec.internal.arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * BitArray 编解码器测试
 *
 * @author zhouhao
 * @since 1.2
 */
public class BitArrayTest {

    private final BitArray codec = new BitArray();

    @Test
    public void testForType() {
        assertEquals(Boolean[].class, codec.forType());
    }

    @Test
    public void testGetId() {
        assertEquals("bit_array", codec.getId());
    }

    @Test
    public void testByteLength() {
        assertEquals(-1, codec.byteLength());
    }

    @Test
    public void testDecodeSingleByte() {
        // 测试单个字节：0b10100000 = 0xA0
        // 应该解码为：[true, false, true, false, false, false, false, false]
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((byte) 0xA0);

        Boolean[] result = codec.decode(buf);

        assertEquals(8, result.length);
        assertTrue(result[0]);   // bit 7
        assertFalse(result[1]);  // bit 6
        assertTrue(result[2]);   // bit 5
        assertFalse(result[3]);  // bit 4
        assertFalse(result[4]);  // bit 3
        assertFalse(result[5]);  // bit 2
        assertFalse(result[6]);  // bit 1
        assertFalse(result[7]);  // bit 0
    }

    @Test
    public void testDecodeAllOnes() {
        // 测试全1字节：0b11111111 = 0xFF
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((byte) 0xFF);

        Boolean[] result = codec.decode(buf);

        assertEquals(8, result.length);
        for (Boolean bit : result) {
            assertTrue(bit);
        }
    }

    @Test
    public void testDecodeAllZeros() {
        // 测试全0字节：0b00000000 = 0x00
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((byte) 0x00);

        Boolean[] result = codec.decode(buf);

        assertEquals(8, result.length);
        for (Boolean bit : result) {
            assertFalse(bit);
        }
    }

    @Test
    public void testDecodeMultipleBytes() {
        // 测试多个字节：[0xAA, 0x55]
        // 0xAA = 0b10101010 -> [true, false, true, false, true, false, true, false]
        // 0x55 = 0b01010101 -> [false, true, false, true, false, true, false, true]
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((byte) 0xAA);
        buf.writeByte((byte) 0x55);

        Boolean[] result = codec.decode(buf);

        assertEquals(16, result.length);
        // 第一个字节
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertTrue(result[2]);
        assertFalse(result[3]);
        assertTrue(result[4]);
        assertFalse(result[5]);
        assertTrue(result[6]);
        assertFalse(result[7]);
        // 第二个字节
        assertFalse(result[8]);
        assertTrue(result[9]);
        assertFalse(result[10]);
        assertTrue(result[11]);
        assertFalse(result[12]);
        assertTrue(result[13]);
        assertFalse(result[14]);
        assertTrue(result[15]);
    }

    @Test
    public void testEncodeSingleByte() {
        // 测试编码单个字节
        // [true, false, true, false, false, false, false, false] -> 0xA0
        Boolean[] bits = {true, false, true, false, false, false, false, false};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(bits, buf);

        assertEquals(1, buf.readableBytes());
        assertEquals((byte) 0xA0, buf.readByte());
    }

    @Test
    public void testEncodeAllOnes() {
        // 测试编码全1
        Boolean[] bits = {true, true, true, true, true, true, true, true};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(bits, buf);

        assertEquals(1, buf.readableBytes());
        assertEquals((byte) 0xFF, buf.readByte());
    }

    @Test
    public void testEncodeAllZeros() {
        // 测试编码全0
        Boolean[] bits = {false, false, false, false, false, false, false, false};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(bits, buf);

        assertEquals(1, buf.readableBytes());
        assertEquals((byte) 0x00, buf.readByte());
    }

    @Test
    public void testEncodeMultipleBytes() {
        // 测试编码多个字节
        Boolean[] bits = {
            true, false, true, false, true, false, true, false,  // 0xAA
            false, true, false, true, false, true, false, true   // 0x55
        };
        ByteBuf buf = Unpooled.buffer();

        codec.encode(bits, buf);

        assertEquals(2, buf.readableBytes());
        assertEquals((byte) 0xAA, buf.readByte());
        assertEquals((byte) 0x55, buf.readByte());
    }

    @Test
    public void testEncodePartialByte() {
        // 测试编码不足8位的数组（应该填充0）
        // [true, false, true] -> 0b10100000 = 0xA0
        Boolean[] bits = {true, false, true};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(bits, buf);

        assertEquals(1, buf.readableBytes());
        assertEquals((byte) 0xA0, buf.readByte());
    }

    @Test
    public void testEncodeWithNullValues() {
        // 测试编码包含null值的数组（null视为false）
        Boolean[] bits = {true, null, false, null, true, false, false, false};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(bits, buf);

        assertEquals(1, buf.readableBytes());
        // true, false, false, false, true, false, false, false = 0b10001000 = 0x88
        assertEquals((byte) 0x88, buf.readByte());
    }

    @Test
    public void testEncodeEmptyArray() {
        // 测试编码空数组
        Boolean[] bits = {};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(bits, buf);

        assertEquals(0, buf.readableBytes());
    }

    @Test
    public void testEncodeNullArray() {
        // 测试编码null数组
        ByteBuf buf = Unpooled.buffer();

        codec.encode(null, buf);

        assertEquals(0, buf.readableBytes());
    }

    @Test
    public void testRoundTrip() {
        // 测试往返转换
        Boolean[] original = {
            true, false, true, true, false, true, false, false,  // 0xB4
            false, true, true, false, true, false, true, true   // 0x6B
        };
        ByteBuf buf = Unpooled.buffer();

        // 编码
        codec.encode(original, buf);

        // 重置读取位置
        buf.readerIndex(0);

        // 解码
        Boolean[] decoded = codec.decode(buf);

        // 验证
        assertEquals(original.length, decoded.length);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testRoundTripPartialByte() {
        // 测试往返转换（不足8位的情况）
        Boolean[] original = {true, false, true, true, false};
        ByteBuf buf = Unpooled.buffer();

        // 编码
        codec.encode(original, buf);

        // 重置读取位置
        buf.readerIndex(0);

        // 解码
        Boolean[] decoded = codec.decode(buf);

        // 验证（解码后会有8位，前5位应该匹配）
        assertTrue(decoded.length >= original.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], decoded[i]);
        }
    }

    @Test
    public void testRoundTripLargeArray() {
        // 测试大数组的往返转换
        int byteCount = 100;
        Boolean[] original = new Boolean[byteCount * 8];
        for (int i = 0; i < original.length; i++) {
            original[i] = (i % 2 == 0);
        }

        ByteBuf buf = Unpooled.buffer();

        // 编码
        codec.encode(original, buf);

        // 重置读取位置
        buf.readerIndex(0);

        // 解码
        Boolean[] decoded = codec.decode(buf);

        // 验证
        assertEquals(original.length, decoded.length);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testSpecificBitPatterns() {
        // 测试特定的位模式
        testBitPattern((byte) 0x01, new boolean[]{false, false, false, false, false, false, false, true});
        testBitPattern((byte) 0x80, new boolean[]{true, false, false, false, false, false, false, false});
        testBitPattern((byte) 0x55, new boolean[]{false, true, false, true, false, true, false, true});
        testBitPattern((byte) 0xAA, new boolean[]{true, false, true, false, true, false, true, false});
        testBitPattern((byte) 0xF0, new boolean[]{true, true, true, true, false, false, false, false});
        testBitPattern((byte) 0x0F, new boolean[]{false, false, false, false, true, true, true, true});
    }

    private void testBitPattern(byte expectedByte, boolean[] bits) {
        // 编码测试
        Boolean[] booleanArray = new Boolean[bits.length];
        for (int i = 0; i < bits.length; i++) {
            booleanArray[i] = bits[i];
        }

        ByteBuf buf = Unpooled.buffer();
        codec.encode(booleanArray, buf);
        assertEquals(expectedByte, buf.readByte());

        // 解码测试
        buf.clear();
        buf.writeByte(expectedByte);
        Boolean[] decoded = codec.decode(buf);
        assertEquals(bits.length, decoded.length);
        for (int i = 0; i < bits.length; i++) {
            assertEquals(bits[i], decoded[i]);
        }
    }
}
