package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.parser.CompositeMessageParser;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link FixedLengthFrameRule} 单元测试.
 * <p>覆盖粘包、拆包场景.</p>
 */
public class FixedLengthFrameRuleTest {

    private static List<ByteBuf> executeRule(MessageFrameRule rule, ByteBuf... payloads) {
        CompositeMessageParser parser = CompositeMessageParser.of(rule);
        List<ByteBuf> result = new ArrayList<>();
        try {
            for (ByteBuf p : payloads) {
                for (EncodedMessage msg : parser.handle(EncodedMessage.simple(p))) {
                    result.add(msg.getPayload());
                }
            }
            return result;
        } finally {
            parser.dispose();
        }
    }

    @Test
    public void matchWhenEnoughBytes() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        ByteBuf buf = Unpooled.copiedBuffer("abcd", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        result.frame.release();
        buf.release();
    }

    @Test
    public void matchWhenNotEnoughBytes() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        ByteBuf buf = Unpooled.copiedBuffer("ab", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void matchWithMatcherReject() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(2, buf -> buf.getByte(buf.readerIndex()) == 0x01);
        ByteBuf buf = Unpooled.buffer(2).writeByte(0x02).writeByte(0x03);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void matchWithMatcherAccept() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(2, buf -> buf.getByte(buf.readerIndex()) == 0x01);
        ByteBuf buf = Unpooled.buffer(2).writeByte(0x01).writeByte(0x02);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsFullFrame() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        ByteBuf buf = Unpooled.copiedBuffer("abcd", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals(4, result.frame.readableBytes());
        assertEquals("abcd", result.frame.toString(StandardCharsets.US_ASCII));
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsNullWhenNotEnough() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        ByteBuf buf = Unpooled.copiedBuffer("ab", StandardCharsets.US_ASCII);
        int idx = buf.readerIndex();
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        assertEquals(idx, buf.readerIndex());
        buf.release();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNonPositiveLength() {
        new FixedLengthFrameRule(0);
    }

    /** 粘包: 一段 payload 内多帧 */
    @Test
    public void stickyPackets() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        ByteBuf buf = Unpooled.copiedBuffer("abcdefgh", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals("abcd", frames.get(0).toString(StandardCharsets.US_ASCII));
            assertEquals("efgh", frames.get(1).toString(StandardCharsets.US_ASCII));
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }

    /** 拆包: 多段 payload 拼成一帧 */
    @Test
    public void splitPackets() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        ByteBuf first = Unpooled.copiedBuffer("ab", StandardCharsets.US_ASCII);
        ByteBuf second = Unpooled.copiedBuffer("cd", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rule, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals("abcd", frames.get(0).toString(StandardCharsets.US_ASCII));
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }
}
