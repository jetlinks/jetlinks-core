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

public class XMLMessageFrameRuleTest {

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
    public void parseSingleElement() {
        XMLMessageFrameRule rule = new XMLMessageFrameRule();
        String xml = "<root><a>1</a></root>";
        ByteBuf buf = Unpooled.copiedBuffer(xml, StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNotNull(result.frame);
            assertEquals(xml, result.frame.toString(StandardCharsets.UTF_8));
        } finally {
            if (result.frame != null) {
                result.frame.release();
            }
            buf.release();
        }
    }

    @Test
    public void stickyPackets() {
        XMLMessageFrameRule rule = new XMLMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("<a>1</a><b>2</b>", StandardCharsets.UTF_8);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals("<a>1</a>", frames.get(0).toString(StandardCharsets.UTF_8));
            assertEquals("<b>2</b>", frames.get(1).toString(StandardCharsets.UTF_8));
        } finally {
            for (ByteBuf frame : frames) {
                frame.release();
            }
        }
    }

    @Test
    public void splitPackets() {
        XMLMessageFrameRule rule = new XMLMessageFrameRule();
        ByteBuf first = Unpooled.copiedBuffer("<root><a>", StandardCharsets.UTF_8);
        ByteBuf second = Unpooled.copiedBuffer("1</a></root>", StandardCharsets.UTF_8);
        List<ByteBuf> frames = executeRule(rule, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals("<root><a>1</a></root>", frames.get(0).toString(StandardCharsets.UTF_8));
        } finally {
            for (ByteBuf frame : frames) {
                frame.release();
            }
        }
    }

    @Test
    public void parseWithDeclarationCommentAndCdata() {
        XMLMessageFrameRule rule = new XMLMessageFrameRule();
        String xml = "<?xml version=\"1.0\"?><root><!--note--><![CDATA[a<b>c]]><item k=\">\">v</item></root>";
        ByteBuf buf = Unpooled.copiedBuffer(xml, StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNotNull(result.frame);
            assertEquals(xml, result.frame.toString(StandardCharsets.UTF_8));
        } finally {
            if (result.frame != null) {
                result.frame.release();
            }
            buf.release();
        }
    }

    @Test
    public void parseWithDoctype() {
        XMLMessageFrameRule rule = new XMLMessageFrameRule();
        String xml = "<!DOCTYPE note [<!ENTITY writer \"Donald Duck\">]><note>&writer;</note>";
        ByteBuf buf = Unpooled.copiedBuffer(xml, StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNotNull(result.frame);
            assertEquals(xml, result.frame.toString(StandardCharsets.UTF_8));
        } finally {
            if (result.frame != null) {
                result.frame.release();
            }
            buf.release();
        }
    }

    @Test
    public void returnNeedMoreWhenIncomplete() {
        XMLMessageFrameRule rule = new XMLMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("<root><a>1</a>", StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNull(result.frame);
            assertEquals(0, result.startIndex);
        } finally {
            buf.release();
        }
    }

    @Test
    public void skipGarbageAndParseXml() {
        XMLMessageFrameRule rule = new XMLMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("xxx<root/>", StandardCharsets.UTF_8);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        try {
            assertNotNull(result.frame);
            assertEquals(3, result.startIndex);
            assertEquals("<root/>", result.frame.toString(StandardCharsets.UTF_8));
        } finally {
            if (result.frame != null) {
                result.frame.release();
            }
            buf.release();
        }
    }

    @Test
    public void recoverAfterMalformedPrefix() {
        XMLMessageFrameRule rule = new XMLMessageFrameRule();
        ByteBuf buf = Unpooled.copiedBuffer("</x><root>1</root>", StandardCharsets.UTF_8);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(1, frames.size());
            assertEquals("<root>1</root>", frames.get(0).toString(StandardCharsets.UTF_8));
        } finally {
            for (ByteBuf frame : frames) {
                frame.release();
            }
        }
    }
}
