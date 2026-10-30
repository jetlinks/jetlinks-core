package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.codec.layout.ByteLayout;
import org.junit.Test;

import static org.junit.Assert.*;

public class AsciiCodecTest {

    private final Codec<String> codec = Codecs.Internal.ASCII;

    @Test
    public void shouldRegisterVariableLengthStringCodec() {
        assertSame(codec, Codecs.getNow("ascii"));
        assertEquals(String.class, codec.forType());
        assertEquals(-1, codec.byteLength());
        assertTrue(codec.isByteLengthSupported(0));
        assertTrue(codec.isByteLengthSupported(6));
    }

    @Test
    public void shouldDecodeReadableRegionAndStopAtNull() {
        ByteBuf buffer = Unpooled.buffer(32);
        try {
            buffer.writeByte(0xff);
            buffer.writeBytes(ByteBufUtil.decodeHexDump("20534e2d3031200000"));
            buffer.skipBytes(1);

            assertEquals(" SN-01 ", codec.decode(buffer));
            assertEquals(buffer.writerIndex(), buffer.readerIndex());
            assertEquals(1, buffer.refCnt());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void shouldStopAtFirstNull() {
        ByteBuf buffer = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("4142004320200000"));
        try {
            assertEquals("AB", codec.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void shouldIgnorePaddingAfterStringTerminator() {
        ByteBuf buffer = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("410042ff"));
        try {
            assertEquals("A", codec.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void shouldRejectNulTextWithoutWritingPartialData() {
        for (String text : new String[]{"\u0000", "AB\u0000", "A\u0000B"}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeByte(0x55);
                try {
                    codec.encode(text, buffer);
                    fail("NUL text must be rejected");
                } catch (BusinessException.NoStackTrace expected) {
                    assertEquals("55", ByteBufUtil.hexDump(buffer));
                }
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    public void shouldAppendAsciiBytesAndRoundTripControlCharacters() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeByte(0xff);
            assertSame(buffer, codec.encode("SN-01 AB\r\n\t", buffer));
            assertEquals("ff534e2d30312041420d0a09", ByteBufUtil.hexDump(buffer));
            buffer.skipBytes(1);

            assertEquals("SN-01 AB\r\n\t", codec.decode(buffer));
            assertEquals(1, buffer.refCnt());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void shouldSupportEmptyAndZeroPaddedEmptyValues() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertSame(buffer, codec.encode("", buffer));
            assertEquals("", codec.decode(buffer));
            buffer.writeZero(4);
            assertEquals("", codec.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
    }

    @Test
    public void shouldRejectNonAsciiBytesWithoutConsumingInput() {
        for (String hex : new String[]{"418000", "c3a9", "ff"}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeByte(0x55);
                buffer.writeBytes(ByteBufUtil.decodeHexDump(hex));
                buffer.skipBytes(1);
                try {
                    codec.decode(buffer);
                    fail("Non-ASCII bytes must be rejected");
                } catch (BusinessException.NoStackTrace expected) {
                    assertEquals(1, buffer.readerIndex());
                    assertEquals(1, buffer.refCnt());
                }
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    public void shouldRejectNonAsciiTextWithoutWritingPartialData() {
        for (String text : new String[]{"A中", "é", "\uD83D\uDE00", "A\uD800B"}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeByte(0x55);
                try {
                    codec.encode(text, buffer);
                    fail("Non-ASCII characters must be rejected");
                } catch (BusinessException.NoStackTrace expected) {
                    assertEquals("55", ByteBufUtil.hexDump(buffer));
                }
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    public void shouldSupportDirectAndCompositeBuffers() {
        ByteBuf direct = Unpooled.directBuffer();
        ByteBuf composite = Unpooled.wrappedBuffer(new byte[]{0x41}, new byte[]{0x42, 0x00, 0x00});
        try {
            direct.writeBytes(new byte[]{0x41, 0x42, 0x00, 0x00});
            assertEquals("AB", codec.decode(direct));
            assertEquals("AB", codec.decode(composite));
            assertEquals(1, direct.refCnt());
            assertEquals(1, composite.refCnt());
        } finally {
            direct.release();
            composite.release();
        }
    }

    @Test
    public void shouldWorkWithCallerAppliedByteLayout() {
        ByteBuf buffer = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("43444142"));
        try {
            ByteLayout.CD_AB.reorder(buffer);
            assertEquals("ABCD", codec.decode(buffer));
            buffer.clear();
            codec.encode("ABCD", buffer);
            ByteLayout.CD_AB.reorder(buffer);
            assertEquals("43444142", ByteBufUtil.hexDump(buffer));
        } finally {
            buffer.release();
        }
    }
}
