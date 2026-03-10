package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.parser.CompositeMessageParser;
import org.jetlinks.core.message.codec.parser.MessageFrameRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link ModbusTcpFrameRule} 单元测试.
 * <p>覆盖粘包、拆包场景.</p>
 */
public class ModbusTcpFrameRuleTest {

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

    /** 标准 Modbus TCP: 事务2 协议0 长度6 UnitId17 功能03 起始107 数量3 */
    private static final byte[] FRAME = new byte[]{
        0x00, 0x01, 0x00, 0x00, 0x00, 0x06, 0x11, 0x03, 0x00, 0x6B, 0x00, 0x03
    };

    @Test
    public void matchRejectsLessThan7Bytes() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0x00, 0x01, 0x00, 0x00, 0x00, 0x06});
        assertFalse(rule.match(buf));
        buf.release();
    }

    @Test
    public void matchAcceptsValidHeader() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(FRAME);
        assertTrue(rule.match(buf));
        buf.release();
    }

    @Test
    public void matchRejectsWrongProtocolIdWhenCheckEnabled() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule(true, 0, buf -> true);
        ByteBuf buf = Unpooled.buffer(7);
        buf.writeShort(0x0001);
        buf.writeShort(0x0001);  // 协议号 1 而非 0
        buf.writeShort(0x0006);
        buf.writeByte(0x11);
        assertFalse(rule.match(buf));
        buf.release();
    }

    @Test
    public void parseReturnsFullFrame() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(FRAME);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(12, frame.readableBytes());
        byte[] actual = new byte[12];
        frame.getBytes(frame.readerIndex(), actual);
        assertArrayEquals(FRAME, actual);
        frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsNullWhenPayloadNotArrived() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule();
        ByteBuf buf = Unpooled.buffer(10);
        buf.writeShort(0x0001);
        buf.writeShort(0x0000);
        buf.writeShort(10);   // Length=10, 需要 6+10=16 字节
        buf.writeByte(0x11);
        buf.writeByte(0x03);
        buf.writeByte(0x00);
        buf.writeByte(0x6B);
        ByteBuf frame = rule.parse(buf);
        assertNull(frame);
        buf.release();
    }

    @Test
    public void parseUsesLengthField() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule();
        ByteBuf buf = Unpooled.buffer(8);
        buf.writeShort(0x0001);
        buf.writeShort(0x0000);
        buf.writeShort(2);   // Length=2 -> 总长 6+2=8
        buf.writeByte(0x01);
        buf.writeByte(0x03);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(8, frame.readableBytes());
        frame.release();
        buf.release();
    }

    /** 粘包: 一段内两帧 */
    @Test
    public void stickyPackets() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule();
        ByteBuf buf = Unpooled.buffer(FRAME.length * 2);
        buf.writeBytes(FRAME);
        buf.writeBytes(FRAME);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals(12, frames.get(0).readableBytes());
            assertEquals(12, frames.get(1).readableBytes());
            byte[] a = new byte[12];
            frames.get(0).getBytes(frames.get(0).readerIndex(), a);
            assertArrayEquals(FRAME, a);
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }

    /** 拆包: 两段拼成一帧 */
    @Test
    public void splitPackets() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule();
        ByteBuf first = Unpooled.wrappedBuffer(new byte[]{0x00, 0x01, 0x00, 0x00, 0x00, 0x06});
        ByteBuf second = Unpooled.wrappedBuffer(new byte[]{0x11, 0x03, 0x00, 0x6B, 0x00, 0x03});
        List<ByteBuf> frames = executeRule(rule, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals(12, frames.get(0).readableBytes());
            byte[] a = new byte[12];
            frames.get(0).getBytes(frames.get(0).readerIndex(), a);
            assertArrayEquals(FRAME, a);
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }
}
