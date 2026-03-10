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

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.US_ASCII);

    @Test
    public void matchWhenDelimiterPresent() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("hello\r\n", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        result.frame.release();
        buf.release();
    }

    @Test
    public void matchWhenDelimiterAbsent() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("hello", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void matchWhenNotEnoughForDelimiter() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("a", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void parseReturnsFrameIncludingDelimiter() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("hello\r\n", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals("hello\r\n", result.frame.toString(StandardCharsets.US_ASCII));
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsNullWhenDelimiterNotPresent() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("hello", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void parseDoesNotConsumeWhenReturnsNull() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF);
        ByteBuf buf = Unpooled.copiedBuffer("he", StandardCharsets.US_ASCII);
        int idx = buf.readerIndex();
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
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
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals("hello", result.frame.toString(StandardCharsets.US_ASCII));
        assertEquals(7, buf.readerIndex()); // consumed "hello" + "\r\n"
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseExcludeDelimiterConsumesDelimiter() {
        DelimiterFrameRule rule = new DelimiterFrameRule(CRLF, true);
        ByteBuf buf = Unpooled.copiedBuffer("a\r\nb\r\n", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result1 = rule.parse(buf);
        assertNotNull(result1.frame);
        assertEquals("a", result1.frame.toString(StandardCharsets.US_ASCII));
        MessageFrameRule.ParseResult result2 = rule.parse(buf);
        assertNotNull(result2.frame);
        assertEquals("b", result2.frame.toString(StandardCharsets.US_ASCII));
        result1.frame.release();
        result2.frame.release();
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
