package org.jetlinks.core.codec.internal.time;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class SegmentedDateTimeCodecTest {

    @Test
    public void test() {
        SegmentedDateTimeCodec codec = new SegmentedDateTimeCodec();
        LocalDateTime time = LocalDateTime.of(2023, 10, 27, 14, 30, 45);

        ByteBuf buf = Unpooled.buffer(12);
        codec.encode(time, buf);

        Assert.assertEquals(12, buf.readableBytes());
        Assert.assertEquals(2023, buf.readUnsignedShort());
        Assert.assertEquals(10, buf.readUnsignedShort());
        Assert.assertEquals(27, buf.readUnsignedShort());
        Assert.assertEquals(14, buf.readUnsignedShort());
        Assert.assertEquals(30, buf.readUnsignedShort());
        Assert.assertEquals(45, buf.readUnsignedShort());

        buf.readerIndex(0);
        LocalDateTime decoded = codec.decode(buf);
        Assert.assertEquals(time, decoded);
    }
}
