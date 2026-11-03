package org.jetlinks.core.codec.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.codec.Codec;
import org.jetlinks.core.codec.Codecs;
import org.jetlinks.core.utils.ConverterUtils;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class UnsignedInt64Test {

    private static final String[] VALUES = {
        "0", "1", "9007199254740993", "9223372036854775807",
        "9223372036854775808", "18446744073709551615"
    };

    private static final String[] HEX = {
        "0000000000000000", "0000000000000001", "0020000000000001",
        "7fffffffffffffff", "8000000000000000", "ffffffffffffffff"
    };

    private final Codec<BigInteger> codec = Codecs.Internal.UnsignedInt64;

    @Test
    public void shouldRegisterScalarAndArray() {
        assertSame(codec, Codecs.getNow("unsigned_int64"));
        assertSame(Codecs.Internal.UnsignedInt64_ARRAY, Codecs.getNow("unsigned_int64_array"));
        assertEquals(BigInteger.class, codec.forType());
        assertEquals(BigInteger[].class, Codecs.Internal.UnsignedInt64_ARRAY.forType());
        assertEquals(8, codec.byteLength());
    }

    @Test
    public void shouldDecodeKnownUnsignedWireValuesWithoutConsumingAdjacentBytes() {
        for (int i = 0; i < VALUES.length; i++) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeByte(0x55);
                buffer.writeBytes(ByteBufUtil.decodeHexDump(HEX[i]));
                buffer.writeByte(0x66);
                buffer.skipBytes(1);
                assertEquals(new BigInteger(VALUES[i]), codec.decode(buffer));
                assertEquals(9, buffer.readerIndex());
                assertEquals(0x66, buffer.readUnsignedByte());
                assertEquals(1, buffer.refCnt());
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    public void shouldEncodeExactEightByteValues() {
        for (int i = 0; i < VALUES.length; i++) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                assertSame(buffer, codec.encode(new BigInteger(VALUES[i]), buffer));
                assertEquals(HEX[i], ByteBufUtil.hexDump(buffer));
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    public void shouldRejectOutOfRangeBeforeWriting() {
        for (BigInteger value : new BigInteger[]{BigInteger.valueOf(-1), BigInteger.ONE.shiftLeft(64)}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                buffer.writeByte(0x55);
                try {
                    codec.encode(value, buffer);
                    fail("Out-of-range UInt64 must be rejected: " + value);
                } catch (BusinessException.NoStackTrace expected) {
                    assertEquals(1, buffer.writerIndex());
                }
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    public void shouldRejectTruncatedWireValue() {
        ByteBuf buffer = Unpooled.wrappedBuffer(new byte[7]);
        try {
            try {
                codec.decode(buffer);
                fail("A UInt64 requires eight bytes");
            } catch (IndexOutOfBoundsException expected) {
                assertEquals(0, buffer.readerIndex());
            }
        } finally {
            buffer.release();
        }
    }

    @Test
    public void shouldConvertDecimalTextAndRoundTripArrayWithoutPrecisionLoss() {
        BigInteger[] values = new BigInteger[VALUES.length];
        for (int i = 0; i < VALUES.length; i++) {
            values[i] = ConverterUtils.convert(VALUES[i], codec.forType());
            assertEquals(new BigInteger(VALUES[i]), values[i]);
        }
        Codec<BigInteger[]> arrayCodec = Codecs.Internal.UnsignedInt64_ARRAY;
        ByteBuf buffer = Unpooled.buffer();
        try {
            arrayCodec.encode(values, buffer);
            assertEquals(String.join("", HEX), ByteBufUtil.hexDump(buffer));
            assertArrayEquals(values, arrayCodec.decode(buffer));
            assertTrue(arrayCodec.isByteLengthSupported(16));
            assertFalse(arrayCodec.isByteLengthSupported(9));
        } finally {
            buffer.release();
        }
    }
}
