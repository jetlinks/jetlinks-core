package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.parser.rule.ModbusRtuFrameRule;
import org.jetlinks.core.message.codec.parser.rule.StartEndFrameRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CompositeMessageParserIntegrationTest {

    private static List<ByteBuf> executeRule(List<MessageFrameRule> rules, ByteBuf... payloads) {
        CompositeMessageParser parser = CompositeMessageParser.of(rules);
        List<ByteBuf> result = new ArrayList<>();
        try {
            for (ByteBuf payload : payloads) {
                List<? extends EncodedMessage> messages = parser.handle(EncodedMessage.simple(payload));
                for (EncodedMessage msg : messages) {
                    result.add(msg.getPayload());
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

    private static void assertFrameBytes(ByteBuf frame, String expectedHex) {
        byte[] expected = ByteBufUtil.decodeHexDump(expectedHex);
        assertEquals("frame length", expected.length, frame.readableBytes());
        byte[] actual = new byte[frame.readableBytes()];
        frame.getBytes(frame.readerIndex(), actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testIntegration() {
        List<MessageFrameRule> rules = new ArrayList<>();
        rules.add(new ModbusRtuFrameRule());
        rules.add(new StartEndFrameRule("@@".getBytes(), "##".getBytes()));

        String[] origins = {
            "50031effe0ffb70830000000000000000000000000fe9e009cda020ae5000000002e98",
            "50031effe1ffba0830000000000000000000000000fe9e009cda020ae50000000011c7",
            "fd1b08020000000000000000000000fe9e009dda020ae600000000e70b", // 错误报文
            "50031effe1ffb80831000000000000000000000000fe9e009cda020ae80000000097ba",
            "50031effe1ffb9082f000000000000000000000000fe9e009cda020ae60000000000b1",
            "feba0832c008000000000000000000fe9e009cda020ae900000000aa74",
            "50031effe1ffba082f000000000000000000000000fe9e009bda020ae5000000006346",
            "50031effe1ffba0831000000000000000000000000fe9e009bda020ae4000000005ece",
            "4040626561742323", // @@beat##
            "50031effdfffbb082e000000000000000000000000fe9e009cda020ae5000000006f23",
            "50031effe2ffb90830000000000000000000000000fe9f009cda020ae7000000002d3e"
        };

        // 将所有报文拼在一起，模拟粘包
        ByteBuf buf = Unpooled.buffer();
        for (String origin : origins) {
            buf.writeBytes(ByteBufUtil.decodeHexDump(origin));
        }

        List<ByteBuf> frames = executeRule(rules, buf);

        try {
            System.out.println("[DEBUG_LOG] Total frames parsed: " + frames.size());
            for (int i = 0; i < frames.size(); i++) {
                System.out.println("[DEBUG_LOG] Frame " + i + ": " + ByteBufUtil.hexDump(frames.get(i)));
            }

            // 预期：
            // - 有 10 条原始报文，其中 1 条 CRC 不合法的“错误报文”应被忽略；
            // - 剩余 9 条报文 + 1 条 @@beat## 心跳。
            assertEquals(9, frames.size());
        } finally {
            releaseAll(frames);
        }
    }
}
