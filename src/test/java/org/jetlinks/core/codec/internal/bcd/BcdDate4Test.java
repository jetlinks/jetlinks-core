package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;

public class BcdDate4Test {

    @Test
    public void test() {
        BcdDate4 codec = new BcdDate4();
        LocalDate date = LocalDate.of(2023, 10, 27);

        ByteBuf buf = Unpooled.buffer(4);
        codec.encode(date, buf);

        Assert.assertEquals(4, buf.readableBytes());
        // 2023 in BCD is 0x2023
        Assert.assertEquals((short) 0x2023, buf.readShort());
        // 10 in BCD is 0x10
        Assert.assertEquals((byte) 0x10, buf.readByte());
        // 27 in BCD is 0x27
        Assert.assertEquals((byte) 0x27, buf.readByte());

        buf.readerIndex(0);
        LocalDate decoded = codec.decode(buf);
        Assert.assertEquals(date, decoded);
    }

    @Test
    public void testEdge() {
        BcdDate4 codec = new BcdDate4();
        LocalDate date = LocalDate.of(1999, 12, 31);

        ByteBuf buf = Unpooled.buffer(4);
        codec.encode(date, buf);

        Assert.assertEquals(4, buf.readableBytes());
        Assert.assertEquals((short) 0x1999, buf.readShort());
        Assert.assertEquals((byte) 0x12, buf.readByte());
        Assert.assertEquals((byte) 0x31, buf.readByte());

        buf.readerIndex(0);
        LocalDate decoded = codec.decode(buf);
        Assert.assertEquals(date, decoded);
    }
}
