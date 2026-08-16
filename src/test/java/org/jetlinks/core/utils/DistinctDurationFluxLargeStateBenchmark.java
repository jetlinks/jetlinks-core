package org.jetlinks.core.utils;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.JavaFlightRecorderProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * LargeState 稳态窗口基准。
 *
 * 每次操作恰好淘汰一个最老 key 并插入一个新 key，使活跃数量保持稳定；碰撞场景用于放大
 * bucket 删除和查询成本，9-key 场景用于验证 Small/Large 边界是否反复重建。
 */
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 5, time = 2)
public class DistinctDurationFluxLargeStateBenchmark {

    @State(Scope.Thread)
    public static class ChurnState {

        @Param({"9", "625", "2500", "25000"})
        int activeKeys;

        @Param({"SPREAD", "COLLISION"})
        String hashPattern;

        private DistinctDurationFlux.DurationStore store;
        private TestTicker ticker;
        private BenchmarkKey[] keys;
        private int cursor;

        @Setup(Level.Trial)
        public void setup() {
            boolean collision = "COLLISION".equals(hashPattern);
            keys = new BenchmarkKey[activeKeys << 1];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = new BenchmarkKey(i, collision);
            }

            ticker = new TestTicker();
            store = new DistinctDurationFlux.DurationStore();
            for (int i = 0; i < activeKeys; i++) {
                ticker.now = i;
                if (!store.add(keys[i], activeKeys, ticker)) {
                    throw new IllegalStateException("failed to prepare key " + i);
                }
            }
            cursor = activeKeys;
        }

        boolean churn() {
            ticker.now++;
            BenchmarkKey key = keys[cursor++];
            if (cursor == keys.length) {
                cursor = 0;
            }
            return store.add(key, activeKeys, ticker);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public boolean steadyWindowChurn(ChurnState state) {
        return state.churn();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public boolean steadyWindowChurnLatency(ChurnState state) {
        return state.churn();
    }

    public static void main(String[] args) throws RunnerException {
        String resultName = args.length > 0 ? args[0] : "large-state";
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        boolean quick = args.length > 2 && Boolean.parseBoolean(args[2]);
        boolean profileJfr = args.length > 3 && Boolean.parseBoolean(args[3]);

        File resultDirectory = new File("target/distinct-duration-benchmark");
        resultDirectory.mkdirs();

        OptionsBuilder options = new OptionsBuilder();
        options
            .include(DistinctDurationFluxLargeStateBenchmark.class.getSimpleName() +
                         ".steadyWindowChurn.*")
            .threads(threads)
            .forks(quick ? 1 : 3)
            .warmupIterations(quick ? 1 : 2)
            .warmupTime(TimeValue.seconds(quick ? 1 : 2))
            .measurementIterations(quick ? 2 : 5)
            .measurementTime(TimeValue.seconds(quick ? 1 : 2))
            .addProfiler(GCProfiler.class)
            .result(new File(resultDirectory, resultName + "-t" + threads + ".json").getPath())
            .resultFormat(ResultFormatType.JSON)
            .jvmArgs("-Xms2g", "-Xmx2g", "-XX:+UseG1GC");

        // Keep JFR opt-in: recording every fork perturbs the sub-microsecond latency distribution.
        if (profileJfr) {
            options.addProfiler(
                JavaFlightRecorderProfiler.class,
                "dir=" + new File(resultDirectory, "jfr-" + resultName + "-t" + threads).getPath());
        }

        new Runner(options.build()).run();
    }

    private static final class TestTicker implements LongSupplier {
        private long now;

        @Override
        public long getAsLong() {
            return now;
        }
    }

    private static final class BenchmarkKey {
        private final int value;
        private final boolean collision;

        private BenchmarkKey(int value, boolean collision) {
            this.value = value;
            this.collision = collision;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BenchmarkKey && ((BenchmarkKey) obj).value == value;
        }

        @Override
        public int hashCode() {
            return collision ? 1 : value;
        }
    }
}
