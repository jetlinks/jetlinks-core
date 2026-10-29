package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.Codecs;
import org.junit.Test;

import static org.junit.Assert.*;

public class HexCodecTest {

    private final Codec<String> codec = Codecs.Internal.HEX;

    @Test
    public void shouldRegisterVariableLengthStringCodec() {
        assertSame(codec, Codecs.getNow("hex"));
        assertEquals(String.class, codec.forType());
        assertEquals(-1, codec.byteLength());
        assertTrue(codec.isByteLengthSupported(6));
    }

    @Test
    public void shouldDecodeReadableBytesAndPreserveLeadingZeros() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeByte(0x55);
            buffer.writeBytes(ByteBufUtil.decodeHexDump("000141a01949"));
            buffer.skipBytes(1);

            assertEquals("000141a01949", codec.decode(buffer));
            assertFalse(buffer.isReadable());
            assertEquals(1, buffer.refCnt());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void shouldRoundTripKnownFloatAndDoublePayloadsAsHex() {
        for (String hex : new String[]{"41a01949", "40340329161f20b2", "000000ff80"}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeByte(0x55);
                assertSame(buffer, codec.encode(hex.toUpperCase(java.util.Locale.ROOT), buffer));
                buffer.skipBytes(1);
                assertEquals(hex, ByteBufUtil.hexDump(buffer));
                assertEquals(hex, codec.decode(buffer));
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    public void shouldSupportEmptyPayload() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertSame(buffer, codec.encode("", buffer));
            assertEquals("", codec.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    public void shouldRejectMalformedHexWithoutWritingPartialBytes() {
        for (String hex : new String[]{"123", "0g", "0x01", "00 01"}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeByte(0x55);
                try {
                    codec.encode(hex, buffer);
                    fail("Expected malformed Hex to be rejected: " + hex);
                } catch (IllegalArgumentException expected) {
                    assertEquals("55", ByteBufUtil.hexDump(buffer));
                }
            } finally {
                buffer.release();
            }
        }
    }
}
