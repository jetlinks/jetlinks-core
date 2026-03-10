package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.parser.rule.DelimiterFrameRule;
import org.jetlinks.core.message.codec.parser.rule.FixedLengthFrameRule;
import org.jetlinks.core.message.codec.parser.rule.LengthFieldFrameRule;
import org.jetlinks.core.message.codec.parser.rule.ModbusRtuFrameRule;
import org.jetlinks.core.message.codec.parser.rule.ModbusTcpFrameRule;
import org.jetlinks.core.message.codec.parser.rule.StartEndFrameRule;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * {@link CompositeMessageParser} 单元测试 (JUnit4).
 *
 * <p>通过 {@link #executeRule(List, ByteBuf...)} 传入规则与报文负载，解析得到 {@link List}{@code <ByteBuf>}，对返回的报文数组进行断言。</p>
 * <p>使用内置 rule（StartEnd、FixedLength、Delimiter、LengthField、ModbusRtu、ModbusTcp）组合覆盖粘包、拆包等场景。</p>
 */
public class CompositeMessageParserTest {

    private static final byte[] RTU_READ_REQ = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC4, 0x0B};
    private static final byte[] RTU_READ_RESP = new byte[]{0x01, 0x03, 0x02, 0x00, 0x00, (byte) 0xF8, (byte) 0x84};
    private static final byte[] RTU_EXCEPTION = new byte[]{0x01, (byte) 0x83, 0x02, (byte) 0xC1, (byte) 0xF0};
    private static final byte[] TCP_FRAME = new byte[]{0x00, 0x01, 0x00, 0x00, 0x00, 0x06, 0x11, 0x03, 0x00, 0x6B, 0x00, 0x03};

    /**
     * 使用指定规则依次处理多段 payload（模拟多次收包），返回解析出的所有帧的 payload 列表。
     * 调用方负责对返回的 ByteBuf 做 release。
     */
    private static List<ByteBuf> executeRule(List<MessageFrameRule.FrameRule> rules, ByteBuf... payloads) {
        CompositeMessageParser parser = CompositeMessageParser.of(rules);
        List<ByteBuf> result = new ArrayList<>();
        try {
            for (ByteBuf payload : payloads) {
                List<? extends EncodedMessage> messages = parser.handle(EncodedMessage.simple(payload));
                for (EncodedMessage msg : messages) {
                    result.add(msg.getPayload().retain());
                }
            }
            return result;
        } finally {
            parser.dispose();
        }
    }

    private static void releaseAll(List<ByteBuf> list) {
        if (list != null) {
            for (ByteBuf b : list) {
                if (b != null && b.refCnt() > 0) {
                    b.release();
                }
            }
        }
    }

    private static void assertFrameBytes(ByteBuf frame, byte[] expected) {
        assertEquals("frame length", expected.length, frame.readableBytes());
        byte[] actual = new byte[frame.readableBytes()];
        frame.getBytes(frame.readerIndex(), actual);
        assertArrayEquals(expected, actual);
    }

    private static void assertFrameUtf8(ByteBuf frame, String expected) {
        assertEquals(expected, frame.toString(StandardCharsets.US_ASCII));
    }

    // ---------- 规则组合: StartEnd + FixedLength(ping) + ModbusRtu + ModbusTcp ----------

    private static List<MessageFrameRule.FrameRule> startEndFixedLengthModbusRules() {
        return Arrays.asList(
            new StartEndFrameRule("r_".getBytes(StandardCharsets.US_ASCII), "_r".getBytes(StandardCharsets.US_ASCII)),
            new FixedLengthFrameRule(4, buf -> buf.readableBytes() >= 4
                && buf.getByte(buf.readerIndex()) == 'p'
                && buf.getByte(buf.readerIndex() + 1) == 'i'
                && buf.getByte(buf.readerIndex() + 2) == 'n'
                && buf.getByte(buf.readerIndex() + 3) == 'g'),
            new ModbusRtuFrameRule(),
            new ModbusTcpFrameRule()
        );
    }

    @Test
    public void startEndAndPingSticky() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("r_1_r".getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes("ping".getBytes(StandardCharsets.US_ASCII));
        List<ByteBuf> frames = executeRule(startEndFixedLengthModbusRules(), buf);
        try {
            assertEquals(2, frames.size());
            assertFrameUtf8(frames.get(0), "r_1_r");
            assertFrameUtf8(frames.get(1), "ping");
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void startEndModbusRtuModbusTcpSticky() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("r_1_r".getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes(RTU_READ_REQ);
        buf.writeBytes(TCP_FRAME);
        List<ByteBuf> frames = executeRule(startEndFixedLengthModbusRules(), buf);
        try {
            assertEquals(3, frames.size());
            assertFrameUtf8(frames.get(0), "r_1_r");
            assertFrameBytes(frames.get(1), RTU_READ_REQ);
            assertFrameBytes(frames.get(2), TCP_FRAME);
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void fourFrameSticky() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("r_1_r".getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes("ping".getBytes(StandardCharsets.US_ASCII));
        buf.writeBytes(RTU_READ_REQ);
        buf.writeBytes(TCP_FRAME);
        List<ByteBuf> frames = executeRule(startEndFixedLengthModbusRules(), buf);
        try {
            assertEquals(4, frames.size());
            assertFrameUtf8(frames.get(0), "r_1_r");
            assertFrameUtf8(frames.get(1), "ping");
            assertFrameBytes(frames.get(2), RTU_READ_REQ);
            assertFrameBytes(frames.get(3), TCP_FRAME);
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void startEndAndPingSplit() {
        List<ByteBuf> frames = executeRule(
            startEndFixedLengthModbusRules(),
            Unpooled.copiedBuffer("r_", StandardCharsets.US_ASCII),
            Unpooled.copiedBuffer("1_rp", StandardCharsets.US_ASCII),
            Unpooled.copiedBuffer("ing", StandardCharsets.US_ASCII)
        );
        try {
            assertEquals(2, frames.size());
            assertFrameUtf8(frames.get(0), "r_1_r");
            assertFrameUtf8(frames.get(1), "ping");
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void modbusRtuSplit() {
        ByteBuf first = Unpooled.buffer();
        first.writeBytes("r_1_r".getBytes(StandardCharsets.US_ASCII));
        first.writeBytes(RTU_READ_REQ, 0, 4);
        ByteBuf second = Unpooled.wrappedBuffer(Arrays.copyOfRange(RTU_READ_REQ, 4, 8));
        List<ByteBuf> frames = executeRule(startEndFixedLengthModbusRules(), first, second);
        try {
            assertEquals(2, frames.size());
            assertFrameUtf8(frames.get(0), "r_1_r");
            assertFrameBytes(frames.get(1), RTU_READ_REQ);
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void modbusTcpSplit() {
        ByteBuf first = Unpooled.buffer();
        first.writeBytes("r_1_r".getBytes(StandardCharsets.US_ASCII));
        first.writeBytes(RTU_READ_REQ);
        first.writeBytes(TCP_FRAME, 0, 5);
        ByteBuf second = Unpooled.wrappedBuffer(Arrays.copyOfRange(TCP_FRAME, 5, 12));
        List<ByteBuf> frames = executeRule(startEndFixedLengthModbusRules(), first, second);
        try {
            assertEquals(3, frames.size());
            assertFrameUtf8(frames.get(0), "r_1_r");
            assertFrameBytes(frames.get(1), RTU_READ_REQ);
            assertFrameBytes(frames.get(2), TCP_FRAME);
        } finally {
            releaseAll(frames);
        }
    }

    // ---------- 仅 Modbus RTU / TCP (粘包 + 拆包) ----------

    @Test
    public void modbusRtuOnlySticky() {
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(new ModbusRtuFrameRule());
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(RTU_READ_REQ);
        buf.writeBytes(RTU_READ_RESP);
        List<ByteBuf> frames = executeRule(rules, buf);
        try {
            assertEquals(2, frames.size());
            assertFrameBytes(frames.get(0), RTU_READ_REQ);
            assertFrameBytes(frames.get(1), RTU_READ_RESP);
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void modbusRtuOnlySplit() {
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(new ModbusRtuFrameRule());
        ByteBuf first = Unpooled.wrappedBuffer(Arrays.copyOfRange(RTU_READ_REQ, 0, 5));
        ByteBuf second = Unpooled.wrappedBuffer(Arrays.copyOfRange(RTU_READ_REQ, 5, 8));
        List<ByteBuf> frames = executeRule(rules, first, second);
        try {
            assertEquals(1, frames.size());
            assertFrameBytes(frames.get(0), RTU_READ_REQ);
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void modbusRtuExceptionSticky() {
        List<ByteBuf> frames = executeRule(
            Arrays.asList(new ModbusRtuFrameRule()),
            Unpooled.wrappedBuffer(RTU_EXCEPTION)
        );
        try {
            assertEquals(1, frames.size());
            assertFrameBytes(frames.get(0), RTU_EXCEPTION);
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void modbusRtuExceptionSplit() {
        ByteBuf first = Unpooled.wrappedBuffer(Arrays.copyOfRange(RTU_EXCEPTION, 0, 3));
        ByteBuf second = Unpooled.wrappedBuffer(Arrays.copyOfRange(RTU_EXCEPTION, 3, 5));
        List<ByteBuf> frames = executeRule(Arrays.asList(new ModbusRtuFrameRule()), first, second);
        try {
            assertEquals(1, frames.size());
            assertFrameBytes(frames.get(0), RTU_EXCEPTION);
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void modbusTcpOnlySticky() {
        List<ByteBuf> frames = executeRule(
            Arrays.asList(new ModbusTcpFrameRule()),
            Unpooled.wrappedBuffer(TCP_FRAME)
        );
        try {
            assertEquals(1, frames.size());
            assertFrameBytes(frames.get(0), TCP_FRAME);
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void modbusTcpOnlySplit() {
        ByteBuf first = Unpooled.wrappedBuffer(Arrays.copyOfRange(TCP_FRAME, 0, 7));
        ByteBuf second = Unpooled.wrappedBuffer(Arrays.copyOfRange(TCP_FRAME, 7, 12));
        List<ByteBuf> frames = executeRule(Arrays.asList(new ModbusTcpFrameRule()), first, second);
        try {
            assertEquals(1, frames.size());
            assertFrameBytes(frames.get(0), TCP_FRAME);
        } finally {
            releaseAll(frames);
        }
    }

    // ---------- DelimiterFrameRule (粘包 + 拆包) ----------

    @Test
    public void delimiterCrlfSticky() {
        byte[] crlf = "\r\n".getBytes(StandardCharsets.US_ASCII);
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(new DelimiterFrameRule(crlf));
        ByteBuf buf = Unpooled.copiedBuffer("line1\r\nline2\r\n", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rules, buf);
        try {
            assertEquals(2, frames.size());
            assertFrameUtf8(frames.get(0), "line1\r\n");
            assertFrameUtf8(frames.get(1), "line2\r\n");
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void delimiterCrlfSplit() {
        byte[] crlf = "\r\n".getBytes(StandardCharsets.US_ASCII);
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(new DelimiterFrameRule(crlf));
        ByteBuf first = Unpooled.copiedBuffer("line1\r", StandardCharsets.US_ASCII);
        ByteBuf second = Unpooled.copiedBuffer("\nline2\r\n", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rules, first, second);
        try {
            assertEquals(2, frames.size());
            assertFrameUtf8(frames.get(0), "line1\r\n");
            assertFrameUtf8(frames.get(1), "line2\r\n");
        } finally {
            releaseAll(frames);
        }
    }

    // ---------- LengthFieldFrameRule (粘包 + 拆包) ----------

    @Test
    public void lengthFieldWithTailSticky() {
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(new LengthFieldFrameRule(2, 2, 4, 2));
        ByteBuf buf = Unpooled.buffer(16);
        buf.writeShort(0x0001);
        buf.writeShort(2);
        buf.writeByte(0xAB);
        buf.writeByte(0xCD);
        buf.writeShort(0x1234);
        buf.writeShort(0x0001);
        buf.writeShort(2);
        buf.writeByte(0xEF);
        buf.writeByte(0x00);
        buf.writeShort(0x5678);
        List<ByteBuf> frames = executeRule(rules, buf);
        try {
            assertEquals(2, frames.size());
            assertEquals(8, frames.get(0).readableBytes());
            assertEquals(8, frames.get(1).readableBytes());
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void lengthFieldWithTailSplit() {
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(new LengthFieldFrameRule(2, 2, 4, 2));
        ByteBuf first = Unpooled.buffer(6);
        first.writeShort(0x0001);
        first.writeShort(2);
        first.writeByte(0xAB);
        first.writeByte(0xCD);
        ByteBuf second = Unpooled.buffer(2);
        second.writeShort(0x1234);
        List<ByteBuf> frames = executeRule(rules, first, second);
        try {
            assertEquals(1, frames.size());
            assertEquals(8, frames.get(0).readableBytes());
        } finally {
            releaseAll(frames);
        }
    }

    // ---------- 混合: StartEnd + Delimiter (粘包 + 拆包) ----------

    @Test
    public void startEndAndDelimiterSticky() {
        byte[] crlf = "\r\n".getBytes(StandardCharsets.US_ASCII);
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(
            new StartEndFrameRule("r_".getBytes(StandardCharsets.US_ASCII), "_r".getBytes(StandardCharsets.US_ASCII)),
            new DelimiterFrameRule(crlf)
        );
        ByteBuf buf = Unpooled.copiedBuffer("r_1_r\r\nping\r\n", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rules, buf);
        try {
            assertEquals(3, frames.size());
            assertFrameUtf8(frames.get(0), "r_1_r");
            assertFrameUtf8(frames.get(1), "\r\n");
            assertFrameUtf8(frames.get(2), "ping\r\n");
        } finally {
            releaseAll(frames);
        }
    }

    @Test
    public void startEndAndDelimiterSplit() {
        byte[] crlf = "\r\n".getBytes(StandardCharsets.US_ASCII);
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(
            new StartEndFrameRule("r_".getBytes(StandardCharsets.US_ASCII), "_r".getBytes(StandardCharsets.US_ASCII)),
            new DelimiterFrameRule(crlf)
        );
        ByteBuf first = Unpooled.copiedBuffer("r_1_", StandardCharsets.US_ASCII);
        ByteBuf second = Unpooled.copiedBuffer("r\r\nping\r\n", StandardCharsets.US_ASCII);
        List<ByteBuf> frames = executeRule(rules, first, second);
        try {
            assertEquals(3, frames.size());
            assertFrameUtf8(frames.get(0), "r_1_r");
            assertFrameUtf8(frames.get(1), "\r\n");
            assertFrameUtf8(frames.get(2), "ping\r\n");
        } finally {
            releaseAll(frames);
        }
    }
}
