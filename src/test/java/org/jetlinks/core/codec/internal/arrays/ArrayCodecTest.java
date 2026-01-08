package org.jetlinks.core.codec.internal.arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.internal.Bool;
import org.jetlinks.core.codec.internal.Int16;
import org.jetlinks.core.codec.internal.Int8;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * ArrayCodec 编解码器测试
 *
 * @author zhouhao
 * @since 1.2
 */
public class ArrayCodecTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullCodec() {
        new ArrayCodec<Byte>(null);
    }

    @Test
    public void testInt8ArrayCodec_Basic() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());

        assertEquals(Byte[].class, codec.forType());
        assertEquals("int8_array", codec.getId());
        assertEquals(-1L, (long) codec.byteLength());
    }

    @Test
    public void testInt8ArrayCodec_IsByteLengthSupported() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());

        // Int8 是固定长度（1字节），所以总长度必须是1的倍数
        assertTrue(codec.isByteLengthSupported(1));
        assertTrue(codec.isByteLengthSupported(2));
        assertTrue(codec.isByteLengthSupported(10));
        assertTrue(codec.isByteLengthSupported(100));
        assertFalse(codec.isByteLengthSupported(0));
        assertFalse(codec.isByteLengthSupported(-1));
    }

    @Test
    public void testInt8ArrayCodec_DecodeSingleElement() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((byte) 0x42);

        Byte[] result = codec.decode(buf);

        assertEquals(1, result.length);
        assertEquals(Byte.valueOf((byte) 0x42), result[0]);
    }

    @Test
    public void testInt8ArrayCodec_DecodeMultipleElements() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((byte) 0x01);
        buf.writeByte((byte) 0x02);
        buf.writeByte((byte) 0x03);

        Byte[] result = codec.decode(buf);

        assertEquals(3, result.length);
        assertEquals(Byte.valueOf((byte) 0x01), result[0]);
        assertEquals(Byte.valueOf((byte) 0x02), result[1]);
        assertEquals(Byte.valueOf((byte) 0x03), result[2]);
    }

    @Test
    public void testInt8ArrayCodec_DecodeEmpty() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        ByteBuf buf = Unpooled.buffer();

        Byte[] result = codec.decode(buf);

        assertEquals(0, result.length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInt8ArrayCodec_DecodeInvalidLength() {
        // 注意：Int8 是1字节，任何长度都是1的倍数，所以这个测试实际上不会触发异常
        // 但我们可以测试一个2字节的编解码器
        ArrayCodec<Short> codec = new ArrayCodec<>(new Int16());
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte((byte) 0x01); // 只有1字节，不是2的倍数

        codec.decode(buf);
    }

    @Test
    public void testInt8ArrayCodec_DecodeLargeArray() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        int count = 100;
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < count; i++) {
            buf.writeByte((byte) i);
        }

        Byte[] result = codec.decode(buf);

        assertEquals(count, result.length);
        for (int i = 0; i < count; i++) {
            assertEquals(Byte.valueOf((byte) i), result[i]);
        }
    }

    @Test
    public void testInt8ArrayCodec_EncodeSingleElement() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        Byte[] body = {(byte) 0x42};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(body, buf);

        assertEquals(1, buf.readableBytes());
        assertEquals((byte) 0x42, (byte) buf.readByte());
    }

    @Test
    public void testInt8ArrayCodec_EncodeMultipleElements() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        Byte[] body = {(byte) 0x01, (byte) 0x02, (byte) 0x03};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(body, buf);

        assertEquals(3, buf.readableBytes());
        assertEquals((byte) 0x01, (byte) buf.readByte());
        assertEquals((byte) 0x02, (byte) buf.readByte());
        assertEquals((byte) 0x03, (byte) buf.readByte());
    }

    @Test
    public void testInt8ArrayCodec_EncodeEmptyArray() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        Byte[] body = {};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(body, buf);

        assertEquals(0, buf.readableBytes());
    }

    @Test
    public void testInt8ArrayCodec_EncodeNullArray() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        ByteBuf buf = Unpooled.buffer();

        codec.encode(null, buf);

        assertEquals(0, buf.readableBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInt8ArrayCodec_EncodeWithNullBuf() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        Byte[] body = {(byte) 0x01};

        codec.encode(body, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInt8ArrayCodec_EncodeWithNullElement() {
        // 测试包含 null 元素的数组
        // Int8 编解码器不支持 null，应该抛出异常
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        Byte[] body = {(byte) 0x01, null, (byte) 0x03};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(body, buf);
    }

    @Test
    public void testInt8ArrayCodec_EncodeLargeArray() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        int count = 100;
        Byte[] body = new Byte[count];
        for (int i = 0; i < count; i++) {
            body[i] = (byte) i;
        }

        ByteBuf buf = Unpooled.buffer();
        codec.encode(body, buf);

        assertEquals(count, buf.readableBytes());
        for (int i = 0; i < count; i++) {
            assertEquals((byte) i, (byte) buf.readByte());
        }
    }

    @Test
    public void testInt8ArrayCodec_RoundTrip() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        Byte[] original = {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0xFF, (byte) 0x00};
        ByteBuf buf = Unpooled.buffer();

        // 编码
        codec.encode(original, buf);

        // 重置读取位置
        buf.readerIndex(0);

        // 解码
        Byte[] decoded = codec.decode(buf);

        // 验证
        assertEquals(original.length, decoded.length);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testInt8ArrayCodec_RoundTripLargeArray() {
        ArrayCodec<Byte> codec = new ArrayCodec<>(new Int8());
        int count = 1000;
        Byte[] original = new Byte[count];
        for (int i = 0; i < count; i++) {
            original[i] = (byte) (i % 256);
        }

        ByteBuf buf = Unpooled.buffer();
        codec.encode(original, buf);
        buf.readerIndex(0);
        Byte[] decoded = codec.decode(buf);

        assertEquals(original.length, decoded.length);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testInt16ArrayCodec_Basic() {
        ArrayCodec<Short> codec = new ArrayCodec<>(new Int16());

        assertEquals(Short[].class, codec.forType());
        assertEquals("int16_array", codec.getId());
        assertEquals(-1L, (long) codec.byteLength());
    }

    @Test
    public void testInt16ArrayCodec_IsByteLengthSupported() {
        ArrayCodec<Short> codec = new ArrayCodec<>(new Int16());

        // Int16 是固定长度（2字节），所以总长度必须是2的倍数
        assertTrue(codec.isByteLengthSupported(2));
        assertTrue(codec.isByteLengthSupported(4));
        assertTrue(codec.isByteLengthSupported(10));
        assertFalse(codec.isByteLengthSupported(1));
        assertFalse(codec.isByteLengthSupported(3));
        assertFalse(codec.isByteLengthSupported(0));
        assertFalse(codec.isByteLengthSupported(-1));
    }

    @Test
    public void testInt16ArrayCodec_DecodeMultipleElements() {
        ArrayCodec<Short> codec = new ArrayCodec<>(new Int16());
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort((short) 0x0102);
        buf.writeShort((short) 0x0304);

        Short[] result = codec.decode(buf);

        assertEquals(2, result.length);
        assertEquals(Short.valueOf((short) 0x0102), result[0]);
        assertEquals(Short.valueOf((short) 0x0304), result[1]);
    }

    @Test
    public void testInt16ArrayCodec_RoundTrip() {
        ArrayCodec<Short> codec = new ArrayCodec<>(new Int16());
        Short[] original = {(short) 0x0102, (short) 0x0304, (short) 0xFFFF, (short) 0x0000};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(original, buf);
        buf.readerIndex(0);
        Short[] decoded = codec.decode(buf);

        assertEquals(original.length, decoded.length);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testBoolArrayCodec_Basic() {
        ArrayCodec<Boolean> codec = new ArrayCodec<>(new Bool());

        assertEquals(Boolean[].class, codec.forType());
        assertEquals("bool_array", codec.getId());
        assertEquals(-1L, (long) codec.byteLength());
    }

    @Test
    public void testBoolArrayCodec_RoundTrip() {
        ArrayCodec<Boolean> codec = new ArrayCodec<>(new Bool());
        Boolean[] original = {true, false, true, false, true};
        ByteBuf buf = Unpooled.buffer();

        codec.encode(original, buf);
        buf.readerIndex(0);
        Boolean[] decoded = codec.decode(buf);

        assertEquals(original.length, decoded.length);
        assertArrayEquals(original, decoded);
    }

    /**
     * 测试动态长度的编解码器
     * 创建一个模拟的动态长度编解码器
     */
    static class DynamicLengthCodec implements Codec<String> {
        @Override
        public Class<String> forType() {
            return String.class;
        }

        @Override
        public String getId() {
            return "dynamic_string";
        }

        @Override
        public int byteLength() {
            return -1; // 动态长度
        }

        @Override
        public String decode(io.netty.buffer.ByteBuf payload) {
            int length = payload.readByte();
            byte[] bytes = new byte[length];
            payload.readBytes(bytes);
            return new String(bytes);
        }

        @Override
        public io.netty.buffer.ByteBuf encode(String body, io.netty.buffer.ByteBuf buf) {
            if (body == null) {
                buf.writeByte(0);
                return buf;
            }
            byte[] bytes = body.getBytes();
            buf.writeByte(bytes.length);
            buf.writeBytes(bytes);
            return buf;
        }
    }

    @Test
    public void testDynamicLengthCodec_IsByteLengthSupported() {
        ArrayCodec<String> dynamicCodec = new ArrayCodec<>(new DynamicLengthCodec());

        // 动态长度应该总是支持
        assertTrue(dynamicCodec.isByteLengthSupported(1));
        assertTrue(dynamicCodec.isByteLengthSupported(10));
        assertTrue(dynamicCodec.isByteLengthSupported(100));
        assertTrue(dynamicCodec.isByteLengthSupported(0));
    }

    @Test
    public void testDynamicLengthCodec_Decode() {
        ArrayCodec<String> dynamicCodec = new ArrayCodec<>(new DynamicLengthCodec());
        ByteBuf buf = Unpooled.buffer();
        
        // 编码 "hello" (5字节) + "world" (5字节)
        buf.writeByte(5);
        buf.writeBytes("hello".getBytes());
        buf.writeByte(5);
        buf.writeBytes("world".getBytes());

        String[] result = dynamicCodec.decode(buf);

        assertEquals(2, result.length);
        assertEquals("hello", result[0]);
        assertEquals("world", result[1]);
    }

    @Test
    public void testDynamicLengthCodec_RoundTrip() {
        ArrayCodec<String> dynamicCodec = new ArrayCodec<>(new DynamicLengthCodec());
        String[] original = {"hello", "world", "test"};
        ByteBuf buf = Unpooled.buffer();

        dynamicCodec.encode(original, buf);
        buf.readerIndex(0);
        String[] decoded = dynamicCodec.decode(buf);

        assertEquals(original.length, decoded.length);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testDynamicLengthCodec_WithNullElement() {
        ArrayCodec<String> dynamicCodec = new ArrayCodec<>(new DynamicLengthCodec());
        String[] original = {"hello", null, "world"};
        ByteBuf buf = Unpooled.buffer();

        // DynamicLengthCodec 支持 null（编码为长度为0的字符串）
        dynamicCodec.encode(original, buf);
        buf.readerIndex(0);
        String[] decoded = dynamicCodec.decode(buf);

        assertEquals(original.length, decoded.length);
        assertEquals("hello", decoded[0]);
        assertEquals("", decoded[1]); // null 被编码为长度为0的字符串
        assertEquals("world", decoded[2]);
    }
}
