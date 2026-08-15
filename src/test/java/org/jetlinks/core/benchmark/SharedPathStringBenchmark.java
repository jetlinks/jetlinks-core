package org.jetlinks.core.benchmark;

import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * {@link SharedPathString} 设备 Topic 构造基准。
 *
 * 基准分别保留静态后缀、动态单段后缀、完整路径追加和字符串解析场景，避免只优化
 * eventId 后掩盖 property/status 等既有快路径回退。
 */
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms1g", "-Xmx1g", "-XX:+UseG1GC"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SharedPathStringBenchmark {

    @State(Scope.Thread)
    public static class TopicState {

        private static final SharedPathString DEVICE_TEMPLATE =
            SharedPathString.of("/device/*/*", false);
        private static final SharedPathString PROPERTY_SUFFIX =
            SharedPathString.of("/message/property/report", false);
        private static final SharedPathString EVENT_SUFFIX =
            SharedPathString.of("/message/event", false);

        String productId;
        String deviceId;
        String eventId;
        String fullEventTopic;
        SeparatedCharSequence eventPrefix;
        SeparatedCharSequence eventTopic;

        @Setup
        public void setup() {
            productId = "product-001";
            deviceId = "device-000001";
            eventId = "temperature_alarm";
            fullEventTopic =
                "/device/product-001/device-000001/message/event/temperature_alarm";
            eventPrefix = DEVICE_TEMPLATE
                .replace(2, productId, 3, deviceId)
                .append(EVENT_SUFFIX);
            eventTopic = eventPrefix.append(eventId);

            // sharedParse 命中场景不混入首次解析和缓存填充成本。
            SharedPathString.of(fullEventTopic);
        }

        SeparatedCharSequence propertyTopic() {
            return DEVICE_TEMPLATE
                .replace(2, productId, 3, deviceId)
                .append(PROPERTY_SUFFIX);
        }

        SeparatedCharSequence eventTopic() {
            return DEVICE_TEMPLATE
                .replace(2, productId, 3, deviceId)
                .append(EVENT_SUFFIX)
                .append(eventId);
        }

    }

    @State(Scope.Thread)
    public static class AppendDepthState {

        private static final SharedPathString BASE = SharedPathString.of("/device", false);

        @Param({"1", "4", "8"})
        int appendDepth;

        String segment;
        String expected;

        @Setup
        public void setup() {
            segment = "temperature_alarm";
            StringBuilder builder = new StringBuilder(BASE.toString());
            for (int i = 0; i < appendDepth; i++) {
                builder.append('/').append(segment);
            }
            expected = builder.toString();
        }

        SeparatedCharSequence appendByDepth() {
            SeparatedCharSequence topic = BASE;
            for (int i = 0; i < appendDepth; i++) {
                topic = topic.append(segment);
            }
            return topic;
        }
    }

    @Benchmark
    public SeparatedCharSequence propertyTopic(TopicState state) {
        return state.propertyTopic();
    }

    @Benchmark
    public int propertyTopicHash(TopicState state) {
        return state.propertyTopic().hashCode();
    }

    @Benchmark
    public SeparatedCharSequence eventTopic(TopicState state) {
        return state.eventTopic();
    }

    @Benchmark
    public int eventTopicHash(TopicState state) {
        return state.eventTopic().hashCode();
    }

    @Benchmark
    public SeparatedCharSequence appendSingleSegment(TopicState state) {
        return state.eventPrefix.append(state.eventId);
    }

    @Benchmark
    public int appendSingleSegmentHash(TopicState state) {
        return state.eventPrefix.append(state.eventId).hashCode();
    }

    @Benchmark
    public SeparatedCharSequence appendFullPath(TopicState state) {
        return SharedPathString
            .of("/device/product-001/device-000001", false)
            .append("/message/event/temperature_alarm");
    }

    @Benchmark
    public int appendDepthHash(AppendDepthState state) {
        return state.appendByDepth().hashCode();
    }

    @Benchmark
    public int appendDepthLength(AppendDepthState state) {
        return state.appendByDepth().length();
    }

    @Benchmark
    public String appendDepthToString(AppendDepthState state) {
        return state.appendByDepth().toString();
    }

    @Benchmark
    public boolean appendDepthContentEquals(AppendDepthState state) {
        return state.appendByDepth().contentEquals(state.expected);
    }

    @Benchmark
    public SharedPathString sharedParse(TopicState state) {
        return SharedPathString.of(state.fullEventTopic);
    }

    @Benchmark
    public SharedPathString unsharedParse(TopicState state) {
        return SharedPathString.of(state.fullEventTopic, false);
    }

    @Benchmark
    public boolean structuredContentEquals(TopicState state) {
        return state.eventTopic.contentEquals(state.fullEventTopic);
    }

    public static void main(String[] args) throws RunnerException {
        String resultName = args.length == 0 ? "benchmark" : args[0];
        boolean quick = args.length > 1 && Boolean.parseBoolean(args[1]);
        File resultDirectory = new File("target/shared-path-string-benchmark");
        resultDirectory.mkdirs();

        OptionsBuilder options = new OptionsBuilder();
        options
            .include(SharedPathStringBenchmark.class.getSimpleName())
            .forks(quick ? 1 : 2)
            .warmupIterations(quick ? 1 : 3)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(quick ? 2 : 5)
            .measurementTime(TimeValue.seconds(1))
            .addProfiler(GCProfiler.class)
            .result(new File(resultDirectory, resultName + ".json").getPath())
            .resultFormat(ResultFormatType.JSON);

        new Runner(options.build()).run();
    }
}
