package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

public class Bcd48Test {

    @Test
    public void test() {
        Bcd48 codec = new Bcd48();
        long val = 123456789012L;

        ByteBuf buf = Unpooled.buffer(6);
        codec.encode(val, buf);

        Assert.assertEquals(6, buf.readableBytes());
        // 0x12 0x34 0x56 0x78 0x90 0x12
        Assert.assertEquals((byte) 0x12, buf.readByte());
        Assert.assertEquals((byte) 0x34, buf.readByte());
        Assert.assertEquals((byte) 0x56, buf.readByte());
        Assert.assertEquals((byte) 0x78, buf.readByte());
        Assert.assertEquals((byte) 0x90, buf.readByte());
        Assert.assertEquals((byte) 0x12, buf.readByte());

        buf.readerIndex(0);
        long decoded = codec.decode(buf);
        Assert.assertEquals(val, decoded);
    }
}
