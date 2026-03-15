package org.jetlinks.core.benchmark;

import lombok.SneakyThrows;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.topic.Topic;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Topic 推送性能压力测试：1000 租户、1000 产品、100 万设备，每订阅 10 个 topic.
 * 对比 String 与 SeparatedCharSequence 两种方式的 findTopic 性能（QPS、GC），并输出报告.
 */
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 2, time = 10)
@Fork(value = 0, jvmArgs = {"-Xms4g", "-Xmx4g", "-XX:+UseG1GC"})
public class TopicTest {

    private static final int TENANT_COUNT = 1000;
    private static final int PRODUCT_COUNT = 1000;
    private static final int TOPICS_PER_SUBSCRIPTION = 10;

    /** 每个订阅包含的 10 个 topic 后缀 */
    private static final String[] TOPIC_SUFFIXES = {
        "message/property/report",
        "message/property/read",
        "message/property/write",
        "message/function/invoke",
        "message/function/invoke/reply",
        "message/event",
        "message/event/up",
        "message/event/down",
        "online",
        "offline"
    };

    /** topic 模版路径：/tenant/0/device/0/0/{suffix}，segment 2=租户ID，4=产品ID */
    private static final String TOPIC_TEMPLATE_PREFIX = "/tenant/0/device/0/0";

    @State(Scope.Benchmark)
    public static class TenantProductDeviceState {
        Topic<Integer> root = Topic.createRoot();
        String[] pushTopicSamples = new String[1024];
        SeparatedCharSequence[] pushTopicSeqSamples = new SeparatedCharSequence[1024];
        /** 按后缀预建的 topic 模版，ExactSeparatedCharSequence 用 replace(2,t).replace(4,p) 构造 */
        SeparatedCharSequence[] topicTemplates = new SeparatedCharSequence[TOPICS_PER_SUBSCRIPTION];

        @Setup(Level.Trial)
        public void init() {
            for (int t = 0; t < TENANT_COUNT; t++) {
                for (int p = 0; p < PRODUCT_COUNT; p++) {
                    int subscriberId = t * PRODUCT_COUNT + p;
                    String devicePath = "/tenant/" + t + "/device/" + p + "/0";
                    for (String suffix : TOPIC_SUFFIXES) {
                        root.append(devicePath + "/" + suffix).subscribe(subscriberId);
                    }
                }
            }
            for (int i = 0; i < TOPIC_SUFFIXES.length; i++) {
                topicTemplates[i] = SharedPathString.of(TOPIC_TEMPLATE_PREFIX + "/" + TOPIC_SUFFIXES[i]);
            }
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < pushTopicSamples.length; i++) {
                int t = r.nextInt(TENANT_COUNT);
                int p = r.nextInt(PRODUCT_COUNT);
                String suffix = TOPIC_SUFFIXES[r.nextInt(TOPICS_PER_SUBSCRIPTION)];
                String topic = "/tenant/" + t + "/device/" + p + "/0/" + suffix;
                pushTopicSamples[i] = topic;
                pushTopicSeqSamples[i] = SharedPathString.of(topic);
            }
        }
    }

    @Benchmark
    @SneakyThrows
    public void pushTenantDeviceSampleString(TenantProductDeviceState state, Blackhole blackhole) {
        String topic = state.pushTopicSamples[ThreadLocalRandom.current().nextInt(state.pushTopicSamples.length)];
        state.root.findTopic(topic,
            node -> blackhole.consume(node.getSubscribers()),
            () -> {});
    }

    @Benchmark
    @SneakyThrows
    public void pushTenantDeviceSampleSeparatedCharSequence(TenantProductDeviceState state, Blackhole blackhole) {
        SeparatedCharSequence topic = state.pushTopicSeqSamples[ThreadLocalRandom.current().nextInt(state.pushTopicSeqSamples.length)];
        state.root.findTopic(topic,
            node -> blackhole.consume(node.getSubscribers()),
            () -> {});
    }

    @Benchmark
    @SneakyThrows
    public void pushTenantDeviceExactString(TenantProductDeviceState state, Blackhole blackhole) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int t = r.nextInt(TENANT_COUNT);
        int p = r.nextInt(PRODUCT_COUNT);
        String suffix = TOPIC_SUFFIXES[r.nextInt(TOPICS_PER_SUBSCRIPTION)];
        String topic = "/tenant/" + t + "/device/" + p + "/0/" + suffix;
        state.root.findTopic(topic,
            node -> blackhole.consume(node.getSubscribers()),
            () -> {});
    }

    /** SeparatedCharSequence：基于模版用 replace 构造 topic，避免每次 of(全量字符串) */
    @Benchmark
    @SneakyThrows
    public void pushTenantDeviceExactSeparatedCharSequence(TenantProductDeviceState state, Blackhole blackhole) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int t = r.nextInt(TENANT_COUNT);
        int p = r.nextInt(PRODUCT_COUNT);
        int suffixIndex = r.nextInt(TOPICS_PER_SUBSCRIPTION);
        SeparatedCharSequence topic = state.topicTemplates[suffixIndex]
            .replace(2, String.valueOf(t),4, String.valueOf(p));
        state.root.findTopic(topic,
            node -> blackhole.consume(node.getSubscribers()),
            () -> {});
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(TopicTest.class.getSimpleName() + "\\.pushTenantDevice.*")
            .threads(4)
            .warmupIterations(2)
            .warmupTime(TimeValue.seconds(5))
            .measurementIterations(2)
            .measurementTime(TimeValue.seconds(10))
            .addProfiler(GCProfiler.class)
            .jvmArgs("-Xms4g", "-Xmx4g", "-XX:+UseG1GC")
            .result("./target/topic-benchmark-result.txt")
            .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.TEXT)
            .build();

        Collection<RunResult> results = new Runner(opt).run();
        String report = buildReport(results);
        System.out.println(report);
    }

    private static String buildReport(Collection<RunResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== Topic 推送压力测试报告 ==========\n\n");
        sb.append("场景: 1000 租户 × 1000 产品 = 100 万设备，每设备 10 个 topic 订阅\n");
        sb.append("对比: String vs SeparatedCharSequence (样本/精确)\n\n");
        for (RunResult r : results) {
            sb.append("--- ").append(r.getParams().getBenchmark()).append(" ---\n");
            sb.append("  ").append(r.getPrimaryResult().getLabel()).append(": ")
                .append(r.getPrimaryResult().toString()).append("\n");
            r.getSecondaryResults().forEach((name, res) ->
                sb.append("  ").append(name).append(": ").append(res.toString()).append("\n"));
            sb.append("\n");
        }
        sb.append("========== 报告结束 ==========\n");
        return sb.toString();
    }
}
