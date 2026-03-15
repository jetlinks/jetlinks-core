package org.jetlinks.core.message.codec.parser.rule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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

    /** 读寄存器请求 01 03 00 00 00 02 CRC */
    private static final byte[] READ_REQ = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC4, 0x0B};

    /** 读寄存器响应 01 03 02 00 00 CRC (Modbus CRC16 低字节在前: 0x44B8 -> B8 44) */
    private static final byte[] READ_RESP = new byte[]{0x01, 0x03, 0x02, 0x00, 0x00, (byte) 0xB8, 0x44};

    /** 异常响应 01 83 02 CRC (Modbus CRC16 低字节在前: 0xF1C0 -> C0 F1) */
    private static final byte[] EXCEPTION = new byte[]{0x01, (byte) 0x83, 0x02, (byte) 0xC0, (byte) 0xF1};

    @Test
    public void matchRejectsLessThan5Bytes() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0x01, 0x03, 0x00, 0x00});
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void matchRejectsUnsupportedFunctionCode() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte(0x01);
        buf.writeByte(0x20);  // 功能码 0x20 不在 1,2,3,4,5,6,15,16
        buf.writeBytes(new byte[]{0x00, 0x00, 0x00});
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void matchAcceptsReadRequest() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(READ_REQ);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseRequest8Bytes() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(READ_REQ);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals(8, result.frame.readableBytes());
        byte[] actual = new byte[8];
        result.frame.getBytes(result.frame.readerIndex(), actual);
        assertArrayEquals(READ_REQ, actual);
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseReturnsNullWhenRequestIncomplete() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00});
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNull(result.frame);
        buf.release();
    }

    @Test
    public void parseResponseWithByteCount() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(READ_RESP);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals(7, result.frame.readableBytes());
        byte[] actual = new byte[7];
        result.frame.getBytes(result.frame.readerIndex(), actual);
        assertArrayEquals(READ_RESP, actual);
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseExceptionResponse5Bytes() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(EXCEPTION);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals(5, result.frame.readableBytes());
        byte[] actual = new byte[5];
        result.frame.getBytes(result.frame.readerIndex(), actual);
        assertArrayEquals(EXCEPTION, actual);
        result.frame.release();
        buf.release();
    }

    @Test
    public void parseWriteMultipleRequest() {
        // 0x10 请求: 地址+功能码+起始(2)+数量(2)+字节数(1)+数据(4)+CRC(2) = 13
        // Modbus CRC16 低字节在前: 前10字节的 CRC 0xAE23 -> 23 AE
        byte[] write10 = new byte[]{
            0x01, 0x10, 0x00, 0x00, 0x00, 0x02, 0x04,
            0x00, 0x01, 0x00, 0x02,
            0x23, (byte) 0xAE
        };
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(write10);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals(13, result.frame.readableBytes());
        result.frame.release();
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

    /**
     * 现场报文: 35 字节 50 03 1e ... 尾 CRC 64 9b.
     * 用当前实现的 Modbus RTU CRC 校验, 若解析出 1 帧则 CRC 通过, 0 帧则 CRC 不通过.
     */
    @Test
    public void parseUserFrame50031effeaffbe() {
        String hex = "50031effeaffbe0830000000000000000000000000feb30068d20f0bd100000000649b";
        byte[] raw = ByteBufUtil.decodeHexDump(hex);
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf buf = Unpooled.wrappedBuffer(raw);
        MessageFrameRule.ParseResult result = rule.parse(buf);
        if (result.frame != null) {
            result.frame.release();
        }
        buf.release();
        // 若你认为该报文 CRC 应对, 则改为 assertNotNull(result.frame); 并修正规则或设备
        assertNotNull("此报文在当前 Modbus CRC 实现下应能解析为一帧", result.frame);
    }

    /** 功能码 0x20 的 35 字节响应：使用正确 CRC 时应收为一帧 */
    @Test
    public void parseFunction20Fixed35Bytes() {
        // 前 33 字节（与用户报文 fa20f4...0ffa 同结构），后 2 字节为 Modbus CRC(前33字节) 低字节在前
        String hex33 = "fa20f4ecffbf0830000000000000000000000000fec3007cd5370bc30000000000";
        byte[] prefix = ByteBufUtil.decodeHexDump(hex33);
        assertEquals("33 bytes for CRC input", 33, prefix.length);
        int crc = calcModbusCrc(Unpooled.wrappedBuffer(prefix), 0, 33);
        ByteBuf buf = Unpooled.buffer(35);
        buf.writeBytes(prefix);
        buf.writeByte(crc & 0xFF);
        buf.writeByte((crc >> 8) & 0xFF);
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        MessageFrameRule.ParseResult result = rule.parse(buf);
        assertNotNull(result.frame);
        assertEquals(35, result.frame.readableBytes());
        result.frame.release();
        buf.release();
    }

    private static int calcModbusCrc(ByteBuf buf, int index, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= buf.getUnsignedByte(index + i);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc = (crc >>> 1);
                }
            }
        }
        return crc & 0xFFFF;
    }
}
