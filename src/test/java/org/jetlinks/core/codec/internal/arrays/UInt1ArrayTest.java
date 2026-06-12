package org.jetlinks.core.codec.internal.arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UInt1ArrayTest {

    private final UInt1Array codec = new UInt1Array();

    @Test
    public void testForType() {
        assertEquals(Integer[].class, codec.forType());
    }

    @Test
    public void testGetId() {
        assertEquals("uint1_array", codec.getId());
    }

    @Test
    public void testDecodeSingleByte() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(0x16);
        Integer[] result = codec.decode(buf);
        assertEquals(8, result.length);
        assertEquals(Integer.valueOf(0), result[0]);
        assertEquals(Integer.valueOf(0), result[1]);
        assertEquals(Integer.valueOf(0), result[2]);
        assertEquals(Integer.valueOf(1), result[3]);
        assertEquals(Integer.valueOf(0), result[4]);
        assertEquals(Integer.valueOf(1), result[5]);
        assertEquals(Integer.valueOf(1), result[6]);
        assertEquals(Integer.valueOf(0), result[7]);
    }

    @Test
    public void testEncodeSingleByte() {
        Integer[] bits = new Integer[]{0, 0, 0, 1, 0, 1, 1, 0};
        ByteBuf buf = Unpooled.buffer();
        codec.encode(bits, buf);
        assertEquals(1, buf.readableBytes());
        assertEquals(0x16, buf.readByte());
    }

    @Test
    public void testRoundTrip() {
        Integer[] bits = new Integer[]{1, 0, 1, 0, 1, 0, 1, 0, 0, 1};
        ByteBuf buf = Unpooled.buffer();
        codec.encode(bits, buf);
        Integer[] decoded = codec.decode(buf);
        for (int i = 0; i < bits.length; i++) {
            assertEquals(bits[i], decoded[i]);
        }
    }
}
