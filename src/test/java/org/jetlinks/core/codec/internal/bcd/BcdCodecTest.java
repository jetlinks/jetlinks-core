package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

public class BcdCodecTest {

    @Test
    public void testBcd8() {
        Bcd8 codec = new Bcd8();
        int value = 25;
        ByteBuf buf = Unpooled.buffer(1);
        codec.encode(value, buf);
        Assert.assertEquals((byte) 0x25, buf.readByte());

        buf.readerIndex(0);
        Assert.assertEquals(Integer.valueOf(value), codec.decode(buf));
    }

    @Test
    public void testBcd16() {
        Bcd16 codec = new Bcd16();
        int value = 1234;
        ByteBuf buf = Unpooled.buffer(2);
        codec.encode(value, buf);
        Assert.assertEquals((short) 0x1234, buf.readShort());

        buf.readerIndex(0);
        Assert.assertEquals(Integer.valueOf(value), codec.decode(buf));
    }

    @Test
    public void testBcd32() {
        Bcd32 codec = new Bcd32();
        int value = 12345678;
        ByteBuf buf = Unpooled.buffer(4);
        codec.encode(value, buf);
        Assert.assertEquals(0x12345678, buf.readInt());

        buf.readerIndex(0);
        Assert.assertEquals(Integer.valueOf(value), codec.decode(buf));
    }

    @Test
    public void testBcdUtils32() {
        int value = 12345678;
        int bcd = BcdUtils.encodeBcd32(value);
        Assert.assertEquals(0x12345678, bcd);
        Assert.assertEquals(value, BcdUtils.decodeBcd(bcd));
    }

    @Test
    public void testUnpackedBcd16() {
        UnpackedBcd16 codec = new UnpackedBcd16();
        int value = 25;
        ByteBuf buf = Unpooled.buffer(2);
        codec.encode(value, buf);
        Assert.assertEquals((short) 0x0205, buf.readShort());

        buf.readerIndex(0);
        Assert.assertEquals(Integer.valueOf(value), codec.decode(buf));
    }

    @Test
    public void testUnpackedBcd32() {
        UnpackedBcd32 codec = new UnpackedBcd32();
        int value = 1234;
        ByteBuf buf = Unpooled.buffer(4);
        codec.encode(value, buf);
        Assert.assertEquals(0x01020304, buf.readInt());

        buf.readerIndex(0);
        Assert.assertEquals(Integer.valueOf(value), codec.decode(buf));
    }
}
