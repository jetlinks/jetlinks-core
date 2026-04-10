package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class BcdDateTime7Test {

    @Test
    public void test() {
        BcdDateTime7 codec = new BcdDateTime7();
        // 2023-10-27 14:30:45 是周五 (DayOfWeek = 5)
        LocalDateTime time = LocalDateTime.of(2023, 10, 27, 14, 30, 45);

        ByteBuf buf = Unpooled.buffer(7);
        codec.encode(time, buf);

        Assert.assertEquals(7, buf.readableBytes());
        // 23 in BCD is 0x23
        Assert.assertEquals((byte) 0x23, buf.readByte());
        // 10 in BCD is 0x10
        Assert.assertEquals((byte) 0x10, buf.readByte());
        // 27 in BCD is 0x27
        Assert.assertEquals((byte) 0x27, buf.readByte());
        // Week 5 in BCD is 0x05
        Assert.assertEquals((byte) 0x05, buf.readByte());
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
}
