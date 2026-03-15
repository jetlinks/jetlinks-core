package org.jetlinks.core.codec.internal.time;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.codec.internal.bcd.BcdDateTime12;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class BcdDateTime12Test {

    @Test
    public void test() {
        BcdDateTime12 codec = new BcdDateTime12();
        LocalDateTime time = LocalDateTime.of(2023, 10, 27, 14, 30, 45);

        ByteBuf buf = Unpooled.buffer(12);
        codec.encode(time, buf);

        Assert.assertEquals(12, buf.readableBytes());
        // 2023 in BCD is 0x2023
        Assert.assertEquals((short) 0x2023, buf.readShort());
        // 10 in BCD is 0x0010
        Assert.assertEquals((short) 0x0010, buf.readShort());
        // 27 in BCD is 0x0027
        Assert.assertEquals((short) 0x0027, buf.readShort());
        // 14 in BCD is 0x0014
        Assert.assertEquals((short) 0x0014, buf.readShort());
        // 30 in BCD is 0x0030
        Assert.assertEquals((short) 0x0030, buf.readShort());
        // 45 in BCD is 0x0045
        Assert.assertEquals((short) 0x0045, buf.readShort());

        buf.readerIndex(0);
        LocalDateTime decoded = codec.decode(buf);
        Assert.assertEquals(time, decoded);
    }
}
