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

import static org.junit.Assert.assertTrue;

/**
 * {@link CompositeMessageParser} 及各类 FrameRule 的性能测试.
 * <p>
 * 通过预热 + 多轮测量计算吞吐（帧/秒）与单帧耗时（纳秒/帧），覆盖粘包、拆包场景。
 * 运行方式: {@code mvn test -Dtest=CompositeMessageParserPerformanceTest}
 * </p>
 */
public class CompositeMessageParserPerformanceTest {

    private static final int WARMUP_ITERATIONS = 20_000;
    private static final int MEASURE_ITERATIONS = 100_000;
    private static final int STICKY_FRAMES_PER_BATCH = 100;

    private static List<ByteBuf> executeRule(MessageFrameRule.FrameRule rule, ByteBuf... payloads) {
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

    private static List<ByteBuf> executeRule(List<MessageFrameRule.FrameRule> rules, ByteBuf... payloads) {
        CompositeMessageParser parser = CompositeMessageParser.of(rules);
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

    private static void releaseAll(List<ByteBuf> list) {
        if (list != null) {
            for (ByteBuf b : list) {
                if (b != null && b.refCnt() > 0) b.release();
            }
        }
    }

    private interface FrameRunnable {
        List<ByteBuf> runAndReturnFrames();
    }

    /** 测量并打印: 场景名、总帧数、迭代次数、帧/秒、纳秒/帧 */
    private static void measure(String scenarioName, int iterations, FrameRunnable runnable) {
        long start = System.nanoTime();
        long totalFrames = 0;
        for (int i = 0; i < iterations; i++) {
            List<ByteBuf> frames = runnable.runAndReturnFrames();
            totalFrames += frames.size();
            releaseAll(frames);
        }
        long elapsedNs = System.nanoTime() - start;
        double fps = totalFrames * 1_000_000_000.0 / elapsedNs;
        double nsPerFrame = elapsedNs / (double) Math.max(1, totalFrames);
        System.out.printf("[性能] %s: 总帧数=%d, 迭代=%d, 耗时=%.2f ms, 吞吐=%.0f 帧/秒, 单帧=%.0f ns%n",
            scenarioName, totalFrames, iterations, elapsedNs / 1_000_000.0, fps, nsPerFrame);
        assertTrue("应解析出帧", totalFrames > 0);
    }

    private void warmup(FrameRunnable run) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            List<ByteBuf> frames = run.runAndReturnFrames();
            releaseAll(frames);
        }
    }

    // ---------- FixedLength 粘包 ----------
    @Test
    public void fixedLengthSticky() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        ByteBuf sticky = Unpooled.buffer(4 * STICKY_FRAMES_PER_BATCH);
        for (int i = 0; i < STICKY_FRAMES_PER_BATCH; i++) {
            sticky.writeByte('a');
            sticky.writeByte('b');
            sticky.writeByte('c');
            sticky.writeByte('d');
        }
        FrameRunnable run = () -> {
            ByteBuf copy = sticky.copy();
            try {
                return executeRule(rule, copy);
            } finally {
                copy.release();
            }
        };
        warmup(run);
        measure("FixedLength-粘包", MEASURE_ITERATIONS, run);
        sticky.release();
    }

    // ---------- FixedLength 拆包 ----------
    @Test
    public void fixedLengthSplit() {
        FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        ByteBuf[] payloads = new ByteBuf[4 * STICKY_FRAMES_PER_BATCH];
        for (int i = 0; i < payloads.length; i++) {
            payloads[i] = Unpooled.wrappedBuffer(new byte[]{(byte) ('a' + (i % 4))});
        }
        FrameRunnable run = () -> {
            for (ByteBuf p : payloads) p.readerIndex(0);
            return executeRule(rule, payloads);
        };
        warmup(run);
        measure("FixedLength-拆包", MEASURE_ITERATIONS / 10, run);
        for (ByteBuf p : payloads) p.release();
    }

    // ---------- Delimiter 粘包 ----------
    @Test
    public void delimiterSticky() {
        DelimiterFrameRule rule = new DelimiterFrameRule("\r\n".getBytes(StandardCharsets.US_ASCII));
        ByteBuf sticky = Unpooled.buffer(STICKY_FRAMES_PER_BATCH * 4);
        for (int i = 0; i < STICKY_FRAMES_PER_BATCH; i++) {
            sticky.writeByte('x');
            sticky.writeByte('\r');
            sticky.writeByte('\n');
        }
        FrameRunnable run = () -> {
            ByteBuf copy = sticky.copy();
            try {
                return executeRule(rule, copy);
            } finally {
                copy.release();
            }
        };
        warmup(run);
        measure("Delimiter-粘包", MEASURE_ITERATIONS, run);
        sticky.release();
    }

    // ---------- LengthField 粘包 ----------
    @Test
    public void lengthFieldSticky() {
        LengthFieldFrameRule rule = new LengthFieldFrameRule(2, 2, 4);
        ByteBuf sticky = Unpooled.buffer(STICKY_FRAMES_PER_BATCH * 6);
        for (int i = 0; i < STICKY_FRAMES_PER_BATCH; i++) {
            sticky.writeShort(0x0001);
            sticky.writeShort(2);
            sticky.writeByte(0xAB);
            sticky.writeByte(0xCD);
        }
        FrameRunnable run = () -> {
            ByteBuf copy = sticky.copy();
            try {
                return executeRule(rule, copy);
            } finally {
                copy.release();
            }
        };
        warmup(run);
        measure("LengthField-粘包", MEASURE_ITERATIONS, run);
        sticky.release();
    }

    // ---------- ModbusRtu 粘包 ----------
    private static final byte[] RTU_FRAME = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC4, 0x0B};

    @Test
    public void modbusRtuSticky() {
        ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        ByteBuf sticky = Unpooled.buffer(RTU_FRAME.length * STICKY_FRAMES_PER_BATCH);
        for (int i = 0; i < STICKY_FRAMES_PER_BATCH; i++) {
            sticky.writeBytes(RTU_FRAME);
        }
        FrameRunnable run = () -> {
            ByteBuf copy = sticky.copy();
            try {
                return executeRule(rule, copy);
            } finally {
                copy.release();
            }
        };
        warmup(run);
        measure("ModbusRtu-粘包", MEASURE_ITERATIONS, run);
        sticky.release();
    }

    // ---------- ModbusTcp 粘包 ----------
    private static final byte[] TCP_FRAME = new byte[]{0x00, 0x01, 0x00, 0x00, 0x00, 0x06, 0x11, 0x03, 0x00, 0x6B, 0x00, 0x03};

    @Test
    public void modbusTcpSticky() {
        ModbusTcpFrameRule rule = new ModbusTcpFrameRule();
        ByteBuf sticky = Unpooled.buffer(TCP_FRAME.length * STICKY_FRAMES_PER_BATCH);
        for (int i = 0; i < STICKY_FRAMES_PER_BATCH; i++) {
            sticky.writeBytes(TCP_FRAME);
        }
        FrameRunnable run = () -> {
            ByteBuf copy = sticky.copy();
            try {
                return executeRule(rule, copy);
            } finally {
                copy.release();
            }
        };
        warmup(run);
        measure("ModbusTcp-粘包", MEASURE_ITERATIONS, run);
        sticky.release();
    }

    // ---------- Composite 多规则粘包 ----------
    @Test
    public void compositeMultiRuleSticky() {
        List<MessageFrameRule.FrameRule> rules = Arrays.asList(
            new StartEndFrameRule("r_".getBytes(StandardCharsets.US_ASCII), "_r".getBytes(StandardCharsets.US_ASCII)),
            new FixedLengthFrameRule(4, buf -> buf.readableBytes() >= 4
                && buf.getByte(buf.readerIndex()) == 'p' && buf.getByte(buf.readerIndex() + 1) == 'i'
                && buf.getByte(buf.readerIndex() + 2) == 'n' && buf.getByte(buf.readerIndex() + 3) == 'g'),
            new ModbusRtuFrameRule(),
            new ModbusTcpFrameRule()
        );
        ByteBuf sticky = Unpooled.buffer();
        for (int i = 0; i < STICKY_FRAMES_PER_BATCH / 4; i++) {
            sticky.writeBytes("r_1_r".getBytes(StandardCharsets.US_ASCII));
            sticky.writeBytes("ping".getBytes(StandardCharsets.US_ASCII));
            sticky.writeBytes(RTU_FRAME);
            sticky.writeBytes(TCP_FRAME);
        }
        FrameRunnable run = () -> {
            ByteBuf copy = sticky.copy();
            try {
                return executeRule(rules, copy);
            } finally {
                copy.release();
            }
        };
        warmup(run);
        measure("Composite-多规则粘包", MEASURE_ITERATIONS, run);
        sticky.release();
    }
}
