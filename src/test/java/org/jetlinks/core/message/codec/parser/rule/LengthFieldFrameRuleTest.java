package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.parser.CompositeMessageParser;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link LengthFieldFrameRule} 单元测试.
 * <p>
 * 示例: 长度字段在偏移 2, 2 字节大端, 头部总长 4, 则帧长 = 4 + length.
 * </p>
 * <p>覆盖粘包、拆包场景.</p>
 */
public class LengthFieldFrameRuleTest {

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

    @Test
    public void matchWhenEnoughForHeaderAndLengthField() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4);
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeShort(0x0001);
        buf.writeShort(2);
        buf.writeByte(0xAB);
        buf.writeByte(0xCD);
        assertTrue(rule.match(buf));
        buf.release();
    }

    @Test
    public void matchWhenNotEnough() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4);
        ByteBuf buf = Unpooled.buffer(3);
        buf.writeShort(0x0001);
        buf.writeByte(0);
        assertFalse(rule.match(buf));
        buf.release();
    }

    @Test
    public void parseReadsLengthFieldAndSlices() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4);
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeShort(0x0001);
        buf.writeShort(2);
        buf.writeByte(0xAB);
        buf.writeByte(0xCD);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(6, frame.readableBytes());
        frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsNullWhenPayloadNotArrived() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4);
        ByteBuf buf = Unpooled.buffer(6);
        buf.writeShort(0x0001);
        buf.writeShort(10);
        buf.writeByte(0);
        buf.writeByte(0);
        buf.writeByte(0);
        buf.writeByte(0);
        ByteBuf frame = rule.parse(buf);
        assertNull(frame);
        buf.release();
    }

    @Test
    public void parseWith1ByteLengthField() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(0, 1, 1);
        ByteBuf buf = Unpooled.buffer(4);
        buf.writeByte(3);
        buf.writeByte(0x11);
        buf.writeByte(0x22);
        buf.writeByte(0x33);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(4, frame.readableBytes());
        frame.release();
        buf.release();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsInvalidLengthFieldLength() {
        new LengthFieldFrameRule(0, 3, 3);
    }

    /**
     * 场景: [header 4 字节][body_len 2 字节][body][crc 2 字节], 长度字段在偏移 2.
     */
    @Test
    public void parseWithTailLength() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4, 2);
        ByteBuf buf = Unpooled.buffer(10);
        buf.writeShort(0x0001);   // 前 2 字节
        buf.writeShort(2);        // body 长度 = 2, 偏移 2
        buf.writeByte(0xAB);
        buf.writeByte(0xCD);      // body
        buf.writeShort(0x1234);   // crc 2 字节
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(8, frame.readableBytes()); // 4 + 2 + 2
        frame.release();
        buf.release();
    }

    @Test
    public void parseWithTailLengthReturnsNullWhenNotEnough() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4, 2);
        ByteBuf buf = Unpooled.buffer(7);
        buf.writeShort(0x0001);
        buf.writeShort(2);
        buf.writeByte(0xAB);
        buf.writeByte(0xCD);
        buf.writeByte(0x12);      // 只有 1 字节尾, 还差 1 字节
        ByteBuf frame = rule.parse(buf);
        assertNull(frame);
        buf.release();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeTailLength() {
        new LengthFieldFrameRule(2, 2, 4, -1);
    }

    /** 粘包: 一段内两帧 (无 tail) */
    @Test
    public void stickyPackets() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4);
        ByteBuf buf = Unpooled.buffer(12);
        buf.writeShort(0x0001);
        buf.writeShort(2);
        buf.writeByte(0xAB);
        buf.writeByte(0xCD);
        buf.writeShort(0x0002);
        buf.writeShort(1);
        buf.writeByte(0x11);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals(6, frames.get(0).readableBytes());
            assertEquals(5, frames.get(1).readableBytes());
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }

    /** 拆包: 两段拼成一帧 */
    @Test
    public void splitPackets() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4);
        ByteBuf first = Unpooled.buffer(4);
        first.writeShort(0x0001);
        first.writeShort(2);
        ByteBuf second = Unpooled.buffer(2);
        second.writeByte(0xAB);
        second.writeByte(0xCD);
        List<ByteBuf> frames = executeRule(rule, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals(6, frames.get(0).readableBytes());
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }
}
