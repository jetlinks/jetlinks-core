package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class BcdDateTime6Test {

    @Test
    public void test() {
        BcdDateTime6 codec = new BcdDateTime6();
        LocalDateTime time = LocalDateTime.of(2023, 10, 27, 14, 30, 45);

        ByteBuf buf = Unpooled.buffer(6);
        codec.encode(time, buf);

        Assert.assertEquals(6, buf.readableBytes());
        // 23 in BCD is 0x23
        Assert.assertEquals((byte) 0x23, buf.readByte());
        // 10 in BCD is 0x10
        Assert.assertEquals((byte) 0x10, buf.readByte());
        // 27 in BCD is 0x27
        Assert.assertEquals((byte) 0x27, buf.readByte());
        // 14 in BCD is 0x14
        Assert.assertEquals((byte) 0x14, buf.readByte());
        // 30 in BCD is 0x30
        Assert.assertEquals((byte) 0x30, buf.readByte());
        // 45 in BCD is 0x45
        Assert.assertEquals((byte) 0x45, buf.readByte());

        buf.readerIndex(0);
        LocalDateTime decoded = codec.decode(buf);
        Assert.assertEquals(time, decoded);
    }

    @Test
    public void testEdge() {
        BcdDateTime6 codec = new BcdDateTime6();
        LocalDateTime time = LocalDateTime.of(2024, 3, 5, 9, 7, 8);

        ByteBuf buf = Unpooled.buffer(6);
        codec.encode(time, buf);

        Assert.assertEquals(6, buf.readableBytes());
        // 24 in BCD is 0x24
        Assert.assertEquals((byte) 0x24, buf.readByte());
        // 03 in BCD is 0x03
        Assert.assertEquals((byte) 0x03, buf.readByte());
        // 05 in BCD is 0x05
        Assert.assertEquals((byte) 0x05, buf.readByte());
        // 09 in BCD is 0x09
        Assert.assertEquals((byte) 0x09, buf.readByte());
        // 07 in BCD is 0x07
        Assert.assertEquals((byte) 0x07, buf.readByte());
        // 08 in BCD is 0x08
        Assert.assertEquals((byte) 0x08, buf.readByte());

        buf.readerIndex(0);
        LocalDateTime decoded = codec.decode(buf);
        Assert.assertEquals(time, decoded);
    }
}
