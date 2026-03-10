package org.jetlinks.core.message.codec.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.parser.rule.FixedLengthFrameRule;
import org.jetlinks.core.message.codec.parser.rule.ModbusRtuFrameRule;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * 解析器 JMH 微基准: 吞吐量（ops/s）、单次耗时.
 * <p>
 * 运行方式一（JUnit 触发）: {@code mvn test -Dtest=ParserJmhBenchmarkTest}
 * 运行方式二（JMH 命令行）: 先 {@code mvn test-compile}，再
 * {@code java -cp "..." org.openjdk.jmh.Main .*ParserJmhBenchmark.* -f 1 -wi 2 -i 2}
 * </p>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ParserJmhBenchmark {

    private static final int FRAMES_PER_BATCH = 100;
    private static final byte[] RTU_FRAME = new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC4, 0x0B};

    @State(Scope.Thread)
    public static class FixedLengthState {
        public final FixedLengthFrameRule rule = new FixedLengthFrameRule(4);
        public final ByteBuf sticky;

        public FixedLengthState() {
            sticky = Unpooled.buffer(4 * FRAMES_PER_BATCH);
            for (int i = 0; i < FRAMES_PER_BATCH; i++) {
                sticky.writeByte('a');
                sticky.writeByte('b');
                sticky.writeByte('c');
                sticky.writeByte('d');
            }
        }
    }

    @State(Scope.Thread)
    public static class ModbusRtuState {
        public final ModbusRtuFrameRule rule = new ModbusRtuFrameRule();
        public final ByteBuf sticky;

        public ModbusRtuState() {
            sticky = Unpooled.buffer(RTU_FRAME.length * FRAMES_PER_BATCH);
            for (int i = 0; i < FRAMES_PER_BATCH; i++) {
                sticky.writeBytes(RTU_FRAME);
            }
        }
    }

    @Benchmark
    public void fixedLengthSticky(FixedLengthState state, Blackhole bh) {
        CompositeMessageParser parser = CompositeMessageParser.of(state.rule);
        try {
            ByteBuf dup = state.sticky.copy();
            for (EncodedMessage msg : parser.handle(EncodedMessage.simple(dup))) {
                bh.consume(msg.getPayload());
                msg.getPayload().release();
            }
            dup.release();
        } finally {
            parser.dispose();
        }
    }

    @Benchmark
    public void modbusRtuSticky(ModbusRtuState state, Blackhole bh) {
        CompositeMessageParser parser = CompositeMessageParser.of(state.rule);
        try {
            ByteBuf dup = state.sticky.copy();
            for (EncodedMessage msg : parser.handle(EncodedMessage.simple(dup))) {
                bh.consume(msg.getPayload());
                msg.getPayload().release();
            }
            dup.release();
        } finally {
            parser.dispose();
        }
    }
}
