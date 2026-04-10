package org.jetlinks.core.codec.internal.arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class LSBBitArrayTest {

    private final LSBBitArray codec = new LSBBitArray();

    @Test
    public void testForType() {
        assertEquals(Boolean[].class, codec.forType());
    }

    @Test
    public void testGetId() {
        assertEquals("lsb_bit_array", codec.getId());
    }

    @Test
    public void testByteLength() {
        assertEquals(-1, codec.byteLength());
    }

    @Test
    public void testDecodeSingleByte() {
        // 0b00000101 (5)
        // LSB first: [T, F, T, F, F, F, F, F]
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x05);
        Boolean[] result = codec.decode(buf);
        assertEquals(8, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertTrue(result[2]);
        assertFalse(result[3]);
        assertFalse(result[4]);
        assertFalse(result[5]);
        assertFalse(result[6]);
        assertFalse(result[7]);
    }

    @Test
    public void testDecodeMultipleBytes() {
        // [0x05, 0x80]
        // 0x05 -> [T, F, T, F, F, F, F, F]
        // 0x80 (10000000) -> [F, F, F, F, F, F, F, T]
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x05);
        buf.writeByte(0x80);
        Boolean[] result = codec.decode(buf);
        assertEquals(16, result.length);
        assertTrue(result[0]);
        assertTrue(result[15]);
        assertFalse(result[1]);
        assertFalse(result[14]);
    }

    @Test
    public void testEncodeSingleByte() {
        Boolean[] bits = new Boolean[]{true, false, true, false, false, false, false, false};
        ByteBuf buf = Unpooled.buffer();
        codec.encode(bits, buf);
        assertEquals(1, buf.readableBytes());
        assertEquals(0x05, buf.readByte());
    }

    @Test
    public void testEncodePartialByte() {
        // 10 bits: [T, F, T, F, F, F, F, F, T, F]
        // Byte 0: 0x05 (00000101)
        // Byte 1: 0x01 (00000001)
        Boolean[] bits = new Boolean[]{true, false, true, false, false, false, false, false, true, false};
        ByteBuf buf = Unpooled.buffer();
        codec.encode(bits, buf);
        assertEquals(2, buf.readableBytes());
        assertEquals(0x05, buf.readByte());
        assertEquals(0x01, buf.readByte());
    }

    @Test
    public void testRoundTrip() {
        Boolean[] bits = new Boolean[20];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = i % 3 == 0;
        }
        ByteBuf buf = Unpooled.buffer();
        codec.encode(bits, buf);

        Boolean[] decoded = codec.decode(buf);
        // BitArray adds padding to 8 bits
        assertTrue(decoded.length >= bits.length);
        for (int i = 0; i < bits.length; i++) {
            assertEquals(bits[i], decoded[i]);
        }
    }
}
