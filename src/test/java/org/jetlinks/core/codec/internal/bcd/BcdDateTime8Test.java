package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class BcdDateTime8Test {

    @Test
    public void test() {
        BcdDateTime8 codec = new BcdDateTime8();
        LocalDateTime time = LocalDateTime.of(2023, 10, 27, 14, 30, 45);

        ByteBuf buf = Unpooled.buffer(8);
        codec.encode(time, buf);

        Assert.assertEquals(8, buf.readableBytes());
        // 2023 in BCD is 0x2023
        Assert.assertEquals((short) 0x2023, buf.readShort());
        // 10/27 in BCD is 0x1027
        Assert.assertEquals((short) 0x1027, buf.readShort());
        // 14/30 in BCD is 0x1430
        Assert.assertEquals((short) 0x1430, buf.readShort());
        // 45 in BCD is 0x0045 (2 bytes)
        Assert.assertEquals((short) 0x0045, buf.readShort());

        buf.readerIndex(0);
        LocalDateTime decoded = codec.decode(buf);
        Assert.assertEquals(time, decoded);
    }

    @Test
    public void testEdge() {
        BcdDateTime8 codec = new BcdDateTime8();
        LocalDateTime time = LocalDateTime.of(2024, 3, 15, 14, 30, 25);

        ByteBuf buf = Unpooled.buffer(8);
        codec.encode(time, buf);

        Assert.assertEquals(8, buf.readableBytes());
        // 2024 in BCD is 0x2024
        Assert.assertEquals((short) 0x2024, buf.readShort());
        // 03/15 in BCD is 0x0315
        Assert.assertEquals((short) 0x0315, buf.readShort());
        // 14/30 in BCD is 0x1430
        Assert.assertEquals((short) 0x1430, buf.readShort());
        // 25 in BCD is 0x0025 (2 bytes)
        Assert.assertEquals((short) 0x0025, buf.readShort());

        buf.readerIndex(0);
        LocalDateTime decoded = codec.decode(buf);
        Assert.assertEquals(time, decoded);
    }
}
