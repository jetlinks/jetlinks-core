package org.jetlinks.core.benchmark;

import org.jetlinks.core.utils.DistinctDurationFlux;
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
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 时间窗口去重操作符基准。
 *
 * 数据热路径和大量短生命周期 distinct 分开测量，避免订阅成本掩盖单条判定成本。
 * JMH 线程之间使用独立订阅，保持 Reactive Streams 对单个 Subscriber 的串行信号约束。
 */
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class DistinctDurationFluxBenchmark {

    private static final Duration TTL = Duration.ofSeconds(30);

    @State(Scope.Thread)
    public static class DataPathState {

        @Param({"NON_FUSEABLE", "FUSEABLE"})
        String sourceType;

        @Param({"REPEAT", "HOT_KEYS", "MIXED", "UNIQUE"})
        String keyPattern;

        @Param("1000000")
        int itemCount;

        Flux<Integer> source;

        @Setup(Level.Trial)
        public void setup() {
            Flux<Integer> values = Flux
                .range(0, itemCount)
                .map(this::selectKey);
            source = "FUSEABLE".equals(sourceType) ? values : values.hide();
        }

        private int selectKey(int value) {
            switch (keyPattern) {
                case "REPEAT":
                    return 0;
                case "HOT_KEYS":
                    return value & 4095;
                case "MIXED":
                    return value % 10 == 0 ? value : value & 63;
                case "UNIQUE":
                    return value;
                default:
                    throw new IllegalArgumentException("unknown key pattern: " + keyPattern);
            }
        }
    }

    @State(Scope.Thread)
    public static class LifecycleState {

        @Param({"0", "1", "8"})
        int dataSize;

        @Param("1000")
        int distinctCount;

        Flux<Integer> source;

        @Setup(Level.Trial)
        public void setup() {
            source = dataSize == 0 ? Flux.empty() : Flux.range(0, dataSize).hide();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public long dataPath(DataPathState state) {
        return DistinctDurationFlux
            .create(state.source, Function.identity(), TTL)
            .count()
            .block();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public long dataPathLatency(DataPathState state) {
        return dataPath(state);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public long shortLivedDistinct(LifecycleState state) {
        long emitted = 0;
        for (int i = 0; i < state.distinctCount; i++) {
            emitted += DistinctDurationFlux
                .create(state.source, Function.identity(), TTL)
                .count()
                .block();
        }
        return emitted;
    }

    public static void main(String[] args) throws RunnerException {
        String suite = args.length > 0 ? args[0] : "data";
        String resultName = args.length > 1 ? args[1] : "benchmark";
        int threads = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        boolean quick = args.length > 3 && Boolean.parseBoolean(args[3]);

        File resultDirectory = new File("target/distinct-duration-benchmark");
        resultDirectory.mkdirs();

        OptionsBuilder options = new OptionsBuilder();
        options
            .include(DistinctDurationFluxBenchmark.class.getSimpleName() +
                         ("lifecycle".equals(suite) ? ".shortLivedDistinct" : ".dataPath.*"))
            .threads(threads)
            .forks(quick ? 1 : 2)
            .warmupIterations(quick ? 1 : 3)
            .warmupTime(TimeValue.seconds(quick ? 1 : 2))
            .measurementIterations(quick ? 2 : 5)
            .measurementTime(TimeValue.seconds(quick ? 1 : 2))
            .addProfiler(GCProfiler.class)
            .result(new File(resultDirectory, resultName + "-t" + threads + ".json").getPath())
            .resultFormat(ResultFormatType.JSON)
            .jvmArgs("-Xms2g", "-Xmx2g", "-XX:+UseG1GC");

        if (!quick) {
            options.addProfiler(
                JavaFlightRecorderProfiler.class,
                "dir=" + new File(resultDirectory, "jfr-" + resultName + "-t" + threads).getPath());
        }

        new Runner(options.build()).run();
    }
}
