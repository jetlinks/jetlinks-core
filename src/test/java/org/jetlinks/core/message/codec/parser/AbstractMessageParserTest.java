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

        TestParser(int maxCumulationBytes, long maxIdleMs, int maxUnparsedBytes) {
            super(maxCumulationBytes, maxIdleMs, maxUnparsedBytes);
        }

        @Override
        protected void handle(ByteBuf buf, List<ByteBuf> container) {
            // 简单实现: 每次尽可能读取全部数据作为一帧
            if (buf.isReadable()) {
                container.add(buf.readRetainedSlice(buf.readableBytes()));
            }
        }
    }

    /** 仅当缓冲区至少 minFrameSize 字节时才取一帧 */
    static class MinSizeParser extends AbstractMessageParser {
        private final int minFrameSize;

        MinSizeParser(int maxCumulationBytes, long maxIdleMs, int maxUnparsedBytes, int minFrameSize) {
            super(maxCumulationBytes, maxIdleMs, maxUnparsedBytes);
            this.minFrameSize = minFrameSize;
        }

        @Override
        protected void handle(ByteBuf buf, List<ByteBuf> container) {
            if (buf.readableBytes() >= minFrameSize) {
                container.add(buf.readRetainedSlice(minFrameSize));
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

    /** 兜底: maxUnparsedBytes 超过阈值时丢弃缓冲区, 下次从新数据重新开始 */
    @Test
    public void shouldResetBufferWhenUnparsedBytesExceedsLimit() {
        int maxUnparsed = 10;
        MinSizeParser parser = new MinSizeParser(1024, 0, maxUnparsed, 20);
        try {
            // 发送 15 字节, 不足 20 不解析, 剩余 15 >= 10 触发重置
            ByteBuf first = Unpooled.buffer(15);
            first.writeZero(15);
            List<? extends EncodedMessage> out1 = parser.handle(EncodedMessage.simple(first));
            assertEquals(0, out1.size());
            first.release();
            // 再发送 20 字节, 应作为全新缓冲区解析出一帧 20 字节(而非 15+20=35)
            ByteBuf second = Unpooled.buffer(20);
            second.writeZero(20);
            List<? extends EncodedMessage> out2 = parser.handle(EncodedMessage.simple(second));
            assertEquals(1, out2.size());
            assertEquals(20, out2.get(0).getPayload().readableBytes());
            out2.get(0).getPayload().release();
            second.release();
        } finally {
            parser.dispose();
        }
    }
}

