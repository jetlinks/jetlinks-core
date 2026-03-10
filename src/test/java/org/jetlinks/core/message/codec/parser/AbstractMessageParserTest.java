package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AbstractMessageParserTest {

    static class TestParser extends AbstractMessageParser {

        TestParser(int maxCumulationBytes) {
            super(maxCumulationBytes);
        }

        @Override
        protected void handle(ByteBuf buf, List<ByteBuf> container) {
            // 简单实现: 每次尽可能读取全部数据作为一帧
            if (buf.isReadable()) {
                container.add(buf.readRetainedSlice(buf.readableBytes()));
            }
        }
    }

    @Test
    public void shouldAcceptWhenUnderLimit() {
        int max = 16;
        TestParser parser = new TestParser(max);
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeZero(8);
        try {
            List<? extends EncodedMessage> messages = parser.handle(EncodedMessage.simple(buf));
            assertEquals(1, messages.size());
            assertTrue(messages.get(0).getLength() <= max);
        } finally {
            parser.dispose();
            buf.release();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenExceedLimit() {
        int max = 16;
        TestParser parser = new TestParser(max);
        ByteBuf buf = Unpooled.buffer(max + 1);
        buf.writeZero(max + 1);
        try {
            parser.handle(EncodedMessage.simple(buf));
        } finally {
            parser.dispose();
            buf.release();
        }
    }
}

