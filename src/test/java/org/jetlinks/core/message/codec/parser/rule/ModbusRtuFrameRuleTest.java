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
 * {@link ModbusRtuFrameRule} 单元测试.
 * <p>覆盖粘包、拆包场景.</p>
 */
public class ModbusRtuFrameRuleTest {

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

    /** 读寄存器请求 01 03 00 00 00 02 CRC */
    private static final byte[] READ_REQ = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC4, 0x0B};

    /** 读寄存器响应 01 03 02 00 00 CRC */
    private static final byte[] READ_RESP = new byte[]{0x01, 0x03, 0x02, 0x00, 0x00, (byte) 0xF8, (byte) 0x84};

    /** 异常响应 01 83 02 CRC */
    private static final byte[] EXCEPTION = new byte[]{0x01, (byte) 0x83, 0x02, (byte) 0xC1, (byte) 0xF0};

    @Test
    public void matchRejectsLessThan5Bytes() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0x01, 0x03, 0x00, 0x00});
        assertFalse(rule.match(buf));
        buf.release();
    }

    @Test
    public void matchRejectsUnsupportedFunctionCode() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte(0x01);
        buf.writeByte(0x20);  // 功能码 0x20 不在 1,2,3,4,5,6,15,16
        buf.writeBytes(new byte[]{0x00, 0x00, 0x00});
        assertFalse(rule.match(buf));
        buf.release();
    }

    @Test
    public void matchAcceptsReadRequest() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(READ_REQ);
        assertTrue(rule.match(buf));
        buf.release();
    }

    @Test
    public void parseRequest8Bytes() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(READ_REQ);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(8, frame.readableBytes());
        byte[] actual = new byte[8];
        frame.getBytes(frame.readerIndex(), actual);
        assertArrayEquals(READ_REQ, actual);
        frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsNullWhenRequestIncomplete() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00});
        ByteBuf frame = rule.parse(buf);
        assertNull(frame);
        buf.release();
    }

    @Test
    public void parseResponseWithByteCount() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(READ_RESP);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(7, frame.readableBytes());
        byte[] actual = new byte[7];
        frame.getBytes(frame.readerIndex(), actual);
        assertArrayEquals(READ_RESP, actual);
        frame.release();
        buf.release();
    }

    @Test
    public void parseExceptionResponse5Bytes() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(EXCEPTION);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(5, frame.readableBytes());
        byte[] actual = new byte[5];
        frame.getBytes(frame.readerIndex(), actual);
        assertArrayEquals(EXCEPTION, actual);
        frame.release();
        buf.release();
    }

    @Test
    public void parseWriteMultipleRequest() {
        // 0x10 请求: 地址+功能码+起始(2)+数量(2)+字节数(1)+数据(4)+CRC(2) = 12
        byte[] write10 = new byte[]{
            0x01, 0x10, 0x00, 0x00, 0x00, 0x02, 0x04,
            0x00, 0x01, 0x00, 0x02,
            (byte) 0xC6, (byte) 0xB0
        };
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(write10);
        ByteBuf frame = rule.parse(buf);
        assertNotNull(frame);
        assertEquals(13, frame.readableBytes());
        frame.release();
        buf.release();
    }

    /** 粘包: 一段内两帧 (请求+响应) */
    @Test
    public void stickyPackets() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.buffer(READ_REQ.length + READ_RESP.length);
        buf.writeBytes(READ_REQ);
        buf.writeBytes(READ_RESP);
        List<ByteBuf> frames = executeRule(rule, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals(8, frames.get(0).readableBytes());
            assertEquals(7, frames.get(1).readableBytes());
            byte[] a = new byte[8];
            frames.get(0).getBytes(frames.get(0).readerIndex(), a);
            assertArrayEquals(READ_REQ, a);
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }

    /** 拆包: 两段拼成一帧 */
    @Test
    public void splitPackets() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf first = Unpooled.wrappedBuffer(new byte[]{0x01, 0x03, 0x00, 0x00});
        ByteBuf second = Unpooled.wrappedBuffer(new byte[]{0x00, 0x02, (byte) 0xC4, 0x0B});
        List<ByteBuf> frames = executeRule(rule, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals(8, frames.get(0).readableBytes());
            byte[] a = new byte[8];
            frames.get(0).getBytes(frames.get(0).readerIndex(), a);
            assertArrayEquals(READ_REQ, a);
        } finally {
            for (ByteBuf b : frames) b.release();
        }
    }
}
