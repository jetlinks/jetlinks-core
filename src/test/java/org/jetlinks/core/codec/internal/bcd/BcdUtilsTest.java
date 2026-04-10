package org.jetlinks.core.codec.internal.bcd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

public class BcdUtilsTest {

    @Test
    public void testBcdString() {
        ByteBuf buf = Unpooled.buffer();
        BcdUtils.writeBcdString(buf, "123456", 3);
        Assert.assertEquals(3, buf.readableBytes());
        Assert.assertEquals((byte) 0x12, buf.readByte());
        Assert.assertEquals((byte) 0x34, buf.readByte());
        Assert.assertEquals((byte) 0x56, buf.readByte());

        buf.clear();
        BcdUtils.writeBcdString(buf, "123", 2);
        Assert.assertEquals(2, buf.readableBytes());
        Assert.assertEquals((byte) 0x01, buf.readByte());
        Assert.assertEquals((byte) 0x23, buf.readByte());

        buf.clear();
        BcdUtils.writeBcdString(buf, "123456", 4);
        Assert.assertEquals(4, buf.readableBytes());
        Assert.assertEquals((byte) 0x00, buf.readByte());
        Assert.assertEquals((byte) 0x12, buf.readByte());
        Assert.assertEquals((byte) 0x34, buf.readByte());
        Assert.assertEquals((byte) 0x56, buf.readByte());

        buf.clear();
        buf.writeByte(0x12);
        buf.writeByte(0x34);
        buf.writeByte(0x56);
        Assert.assertEquals("123456", BcdUtils.readBcdString(buf, 3));

        buf.clear();
        buf.writeByte(0x01);
        buf.writeByte(0x23);
        Assert.assertEquals("0123", BcdUtils.readBcdString(buf, 2));

        buf.clear();
        buf.writeByte(0x01);
        buf.writeByte(0x34);
        Assert.assertEquals("134", BcdUtils.readBcdString(buf, 2, 3));

        buf.clear();
        buf.writeByte(0x00);
        buf.writeByte(0x13);
        buf.writeByte(0x40);
        Assert.assertEquals("1340", BcdUtils.readBcdString(buf, 3, 0));

        buf.clear();
        buf.writeByte(0x00);
        buf.writeByte(0x00);
        Assert.assertEquals("0", BcdUtils.readBcdString(buf, 2, 0));

        buf.clear();
        buf.writeByte(0x01);
        buf.writeByte(0x02);
        Assert.assertEquals("0102", BcdUtils.readBcdString(buf, 2, 4));

        buf.clear();
        buf.writeByte(0x01);
        buf.writeByte(0x02);
        Assert.assertEquals("102", BcdUtils.readBcdString(buf, 2, 3));

        // 验证 writeBcdString 奇数长度支持
        buf.clear();
        BcdUtils.writeBcdString(buf, "123", 2); // 0x01, 0x23
        Assert.assertEquals(2, buf.readableBytes());
        Assert.assertEquals((byte) 0x01, buf.readByte());
        Assert.assertEquals((byte) 0x23, buf.readByte());

        buf.clear();
        BcdUtils.writeBcdString(buf, "1", 1); // 0x01
        Assert.assertEquals(1, buf.readableBytes());
        Assert.assertEquals((byte) 0x01, buf.readByte());

        buf.clear();
        BcdUtils.writeBcdString(buf, "12345", 3); // 0x01, 0x23, 0x45
        Assert.assertEquals(3, buf.readableBytes());
        Assert.assertEquals((byte) 0x01, buf.readByte());
        Assert.assertEquals((byte) 0x23, buf.readByte());
        Assert.assertEquals((byte) 0x45, buf.readByte());
    }
}
