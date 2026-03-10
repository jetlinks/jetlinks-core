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
 * {@link DelimiterFrameRule} 单元测试.
 * <p>覆盖粘包、拆包场景.</p>
 */
public class DelimiterFrameRuleTest {

    private static List<ByteBuf> executeRule(MessageFrameRule.FrameRule rule, ByteBuf... payloads) {
        CompositeMessageParser parser = CompositeMessageParser.of(rule);
        List<ByteBuf> result = new ArrayList<>();
        try {
            for (ByteBuf p : payloads) {
                for (EncodedMessage msg : parser.handle(EncodedMessage.simple(p))) {
                    result.add(msg.getPayload().retain());
                }
            }
            return result;
        } finally {
            parser.dispose();
        }
    }

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);

    @Test
    public void matchWhenDelimiterPresent() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("hello\r\n", StandardCharsets.US_ASCII);
        assertTrue(rule.match(buf));
        buf.release();
    }

    @Test
    public void matchWhenDelimiterAbsent() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("hello", StandardCharsets.US_ASCII);
        assertFalse(rule.match(buf));
        buf.release();
    }

    @Test
    public void matchWhenNotEnoughForDelimiter() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("a", StandardCharsets.US_ASCII);
        assertFalse(rule.match(buf));
        buf.release();
    }

    @Test
    public void parseReturnsFrameIncludingDelimiter() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("hello\r\n", StandardCharsets.US_ASCII);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals("hello\r\n", frame.toString(StandardCharsets.US_ASCII));
        frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsNullWhenDelimiterNotPresent() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("hello", StandardCharsets.US_ASCII);
        ByteBuf frame = rule.parse(buf);
        assertNull(frame);
        buf.release();
    }

    @Test
    public void parseDoesNotConsumeWhenReturnsNull() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("he", StandardCharsets.US_ASCII);
        int idx = buf.readerIndex();
        ByteBuf frame = rule.parse(buf);
        assertNull(frame);
        assertEquals(idx, buf.readerIndex());
        buf.release();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsEmptyDelimiter() {
        new DelimiterFrameRule(new byte[0]);
    }

    @Test
    public void parseExcludeDelimiterReturnsFrameWithoutDelimiter() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF, true);
        ByteBuf buf = Unpooled.copiedBuffer("hello\r\n", StandardCharsets.US_ASCII);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals("hello", frame.toString(StandardCharsets.US_ASCII));
        assertEquals(7, buf.readerIndex()); // consumed "hello" + "\r\n"
        frame.release();
        buf.release();
    }

    @Test
    public void parseExcludeDelimiterConsumesDelimiter() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF, true);
        ByteBuf buf = Unpooled.copiedBuffer("a\r\nb\r\n", StandardCharsets.US_ASCII);
        ByteBuf frame1 = rule.parse(buf);
        assertNotNull(frame1);
        assertEquals("a", frame1.toString(StandardCharsets.US_ASCII));
        ByteBuf frame2 = rule.parse(buf);
        assertNotNull(frame2);
        assertEquals("b", frame2.toString(StandardCharsets.US_ASCII));
        frame1.release();
        frame2.release();
        buf.release();
    }

    /** 粘包: 一段内多行 */
    @Test
    public void stickyPackets() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("a\r\nb\r\n", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals("a\r\n", frames.get(0).toString(StandardCharsets.US_ASCII));
            assertEquals("b\r\n", frames.get(1).toString(StandardCharsets.US_ASCII));
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }

    /** 拆包: 分隔符跨两段 */
    @Test
    public void splitPackets() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf first = Unpooled.copiedBuffer("hello\r", StandardCharsets.US_ASCII);
        ByteBuf second = Unpooled.copiedBuffer("\n", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rule, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals("hello\r\n", frames.get(0).toString(StandardCharsets.US_ASCII));
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }
}
