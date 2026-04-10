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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JSONMessageFrameRuleTest {

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
    public void parseSingleObject() {
        JSONMessageFrameRule rule = new JSONMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("{\"id\":\"1\"}", StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNotNull(result.frame);
            assertEquals("{\"id\":\"1\"}", result.frame.toString(StandardCharsets.UTF_8));
        } finally {
            if (result.frame != null) {
                result.frame.release();
            }
            buf.release();
        }
    }

    @Test
    public void stickyPackets() {
        JSONMessageFrameRule rule = new JSONMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("{\"a\":1}{\"b\":2}", StandardCharsets.UTF_8);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals("{\"a\":1}", frames.get(0).toString(StandardCharsets.UTF_8));
            assertEquals("{\"b\":2}", frames.get(1).toString(StandardCharsets.UTF_8));
        } finally {
            for (ByteBuf frame : frames) {
                frame.release();
            }
        }
    }

    @Test
    public void splitPackets() {
        JSONMessageFrameRule rule = new JSONMessageFrameRule();
        ByteBuf first = Unpooled.copiedBuffer("{\"a\":", StandardCharsets.UTF_8);
        ByteBuf second = Unpooled.copiedBuffer("1}", StandardCharsets.UTF_8);
        List<ByteBuf> frames = executeRule(rule, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals("{\"a\":1}", frames.get(0).toString(StandardCharsets.UTF_8));
        } finally {
            for (ByteBuf frame : frames) {
                frame.release();
            }
        }
    }

    @Test
    public void supportNestedAndEscapedContent() {
        JSONMessageFrameRule rule = new JSONMessageFrameRule();
        String json = "{\"msg\":\"brace:{\\\\\\\"a\\\\\\\"}\",\"arr\":[1,{\"k\":2}]}";
        ByteBuf buf = Unpooled.copiedBuffer(json, StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNotNull(result.frame);
            assertEquals(json, result.frame.toString(StandardCharsets.UTF_8));
        } finally {
            if (result.frame != null) {
                result.frame.release();
            }
            buf.release();
        }
    }

    @Test
    public void parseArrayRoot() {
        JSONMessageFrameRule rule = new JSONMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("[{\"a\":1},{\"b\":2}]", StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNotNull(result.frame);
            assertEquals("[{\"a\":1},{\"b\":2}]", result.frame.toString(StandardCharsets.UTF_8));
        } finally {
            if (result.frame != null) {
                result.frame.release();
            }
            buf.release();
        }
    }

    @Test
    public void returnNeedMoreWhenJsonIncomplete() {
        JSONMessageFrameRule rule = new JSONMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("{\"a\":[1,2", StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNull(result.frame);
            assertEquals(0, result.startIndex);
        } finally {
            buf.release();
        }
    }

    @Test
    public void skipGarbageAndParseValidJson() {
        JSONMessageFrameRule rule = new JSONMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("xxx{\"a\":1}", StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNotNull(result.frame);
            assertEquals(3, result.startIndex);
            assertEquals("{\"a\":1}", result.frame.toString(StandardCharsets.UTF_8));
        } finally {
            if (result.frame != null) {
                result.frame.release();
            }
            buf.release();
        }
    }

    @Test
    public void recoverAfterMalformedPrefix() {
        JSONMessageFrameRule rule = new JSONMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("{] {\"a\":1}", StandardCharsets.UTF_8);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(1, frames.size());
            assertEquals("{\"a\":1}", frames.get(0).toString(StandardCharsets.UTF_8));
        } finally {
            for (ByteBuf frame : frames) {
                frame.release();
            }
        }
    }
}
