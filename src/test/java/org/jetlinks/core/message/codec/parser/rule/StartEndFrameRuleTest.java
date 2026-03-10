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
 * {@link StartEndFrameRule} 单元测试.
 * <p>覆盖粘包、拆包场景.</p>
 */
public class StartEndFrameRuleTest {

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
    public void matchWhenStartAndEndPresent() {
        StartEndFrameRule rule = new StartEndFrameRule(
            "r_".getBytes(StandardCharsets.US_ASCII),
            "_r".getBytes(StandardCharsets.US_ASCII)
        );
        ByteBuf buf = Unpooled.copiedBuffer("r_1_r", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        result.frame.release();
        buf.release();
    }

    @Test
    public void matchWhenOnlyStartPresent() {
        StartEndFrameRule rule = new StartEndFrameRule(
            "r_".getBytes(StandardCharsets.US_ASCII),
            "_r".getBytes(StandardCharsets.US_ASCII)
        );
        ByteBuf buf = Unpooled.copiedBuffer("r_1", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void matchWhenWrongStart() {
        StartEndFrameRule rule = new StartEndFrameRule(
            "r_".getBytes(StandardCharsets.US_ASCII),
            "_r".getBytes(StandardCharsets.US_ASCII)
        );
        ByteBuf buf = Unpooled.copiedBuffer("x_1_r", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void parseReturnsFullFrameIncludingStartAndEnd() {
        StartEndFrameRule rule = new StartEndFrameRule(
            "r_".getBytes(StandardCharsets.US_ASCII),
            "_r".getBytes(StandardCharsets.US_ASCII)
        );
        ByteBuf buf = Unpooled.copiedBuffer("r_1_r", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals("r_1_r", result.frame.toString(StandardCharsets.US_ASCII));
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsNullWhenEndNotYetArrived() {
        StartEndFrameRule rule = new StartEndFrameRule(
            "r_".getBytes(StandardCharsets.US_ASCII),
            "_r".getBytes(StandardCharsets.US_ASCII)
        );
        ByteBuf buf = Unpooled.copiedBuffer("r_1", StandardCharsets.US_ASCII);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void parseDoesNotConsumeWhenReturnsNull() {
        StartEndFrameRule rule = new StartEndFrameRule(
            "r_".getBytes(StandardCharsets.US_ASCII),
            "_r".getBytes(StandardCharsets.US_ASCII)
        );
        ByteBuf buf = Unpooled.copiedBuffer("r_", StandardCharsets.US_ASCII);
        int idx = buf.readerIndex();
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        assertEquals(idx, buf.readerIndex());
        buf.release();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsEmptyStart() {
        new StartEndFrameRule(new byte[0], "_r".getBytes(StandardCharsets.US_ASCII));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsEmptyEnd() {
        new StartEndFrameRule("r_".getBytes(StandardCharsets.US_ASCII), new byte[0]);
    }

    /** 粘包: 一段内两帧 */
    @Test
    public void stickyPackets() {
        StartEndFrameRule rule = new StartEndFrameRule(
            "r_".getBytes(StandardCharsets.US_ASCII),
            "_r".getBytes(StandardCharsets.US_ASCII)
        );
        ByteBuf buf = Unpooled.copiedBuffer("r_1_rr_2_r", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals("r_1_r", frames.get(0).toString(StandardCharsets.US_ASCII));
            assertEquals("r_2_r", frames.get(1).toString(StandardCharsets.US_ASCII));
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }

    /** 拆包: 结尾符跨两段 */
    @Test
    public void splitPackets() {
        StartEndFrameRule rule = new StartEndFrameRule(
            "r_".getBytes(StandardCharsets.US_ASCII),
            "_r".getBytes(StandardCharsets.US_ASCII)
        );
        ByteBuf first = Unpooled.copiedBuffer("r_1", StandardCharsets.US_ASCII);
        ByteBuf second = Unpooled.copiedBuffer("_r", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rule, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals("r_1_r", frames.get(0).toString(StandardCharsets.US_ASCII));
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }
}
