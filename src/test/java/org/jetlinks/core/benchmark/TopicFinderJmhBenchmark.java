package org.jetlinks.core.benchmark;

import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.topic.Topic;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import reactor.function.Consumer4;
import reactor.function.Consumer5;

import java.util.concurrent.TimeUnit;

/**
 * TopicFinder 精确、miss 与搜索侧 wildcard 路径的微基准。
 *
 * 基准使用确定性样本游标，避免把随机数生成成本计入查找；EXACT_ONLY 与 MIXED
 * 分别隔离纯精确树和包含订阅侧 * / ** 的真实混合树。
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3, jvmArgs = {"-Xms2g", "-Xmx2g", "-XX:+UseG1GC"})
public class TopicFinderJmhBenchmark {

    private static final int TENANT_COUNT = 256;
    private static final int PRODUCT_COUNT = 128;
    private static final int SAMPLE_COUNT = 1024;
    private static final int SAMPLE_MASK = SAMPLE_COUNT - 1;
    private static final String EXACT_SUFFIX = "/message/property/report";

    private static final Consumer5<TopicFinderState, Object, Object, Object, Topic<Integer>> MATCH_SINK =
        (state, ignore1, ignore2, ignore3, topic) -> state.matches += topic.getSubscribers().size();

    private static final Consumer4<TopicFinderState, Object, Object, Object> END_SINK =
        (state, ignore1, ignore2, ignore3) -> {
        };

    @State(Scope.Thread)
    public static class TopicFinderState {

        @Param({"EXACT_ONLY", "MIXED"})
        String treeType;

        private Topic<Integer> root;
        private String[] exactTopicSamples;
        private SeparatedCharSequence[] exactSeqSamples;
        private String[] missTopicSamples;
        private SeparatedCharSequence[] missSeqSamples;
        private String[] wildcardTopicSamples;
        private SeparatedCharSequence[] wildcardSeqSamples;
        private int cursor;
        private int matches;

        @Setup(Level.Trial)
        public void init() {
            root = Topic.createRoot();
            exactTopicSamples = new String[SAMPLE_COUNT];
            exactSeqSamples = new SeparatedCharSequence[SAMPLE_COUNT];
            missTopicSamples = new String[SAMPLE_COUNT];
            missSeqSamples = new SeparatedCharSequence[SAMPLE_COUNT];
            wildcardTopicSamples = new String[SAMPLE_COUNT];
            wildcardSeqSamples = new SeparatedCharSequence[SAMPLE_COUNT];

            boolean mixed = "MIXED".equals(treeType);
            for (int tenant = 0; tenant < TENANT_COUNT; tenant++) {
                if (mixed) {
                    root.append("/tenant/" + tenant + "/device/**").subscribe(tenant);
                }
                for (int product = 0; product < PRODUCT_COUNT; product++) {
                    root.append("/tenant/" + tenant + "/device/" + product + EXACT_SUFFIX)
                        .subscribe(product);
                    if (mixed) {
                        root.append("/tenant/" + tenant + "/device/*" + EXACT_SUFFIX)
                            .subscribe(product);
                        root.append("/tenant/" + tenant + "/device/" + product + "/message/*")
                            .subscribe(product);
                    }
                }
            }

            for (int i = 0; i < SAMPLE_COUNT; i++) {
                int tenant = i & (TENANT_COUNT - 1);
                int product = (i * 31) & (PRODUCT_COUNT - 1);
                String exact = "/tenant/" + tenant + "/device/" + product + EXACT_SUFFIX;
                String miss = "/tenant/" + tenant + "/device/missing-" + i + EXACT_SUFFIX;
                String wildcard = "/tenant/" + tenant + "/device/*" + EXACT_SUFFIX;
                exactTopicSamples[i] = exact;
                exactSeqSamples[i] = SharedPathString.of(exact);
                missTopicSamples[i] = miss;
                missSeqSamples[i] = SharedPathString.of(miss);
                wildcardTopicSamples[i] = wildcard;
                wildcardSeqSamples[i] = SharedPathString.of(wildcard);
            }
        }

        private int nextIndex() {
            return cursor++ & SAMPLE_MASK;
        }

        private int find(String topic) {
            matches = 0;
            root.findTopic(topic, this, null, null, null, MATCH_SINK, END_SINK);
            return matches;
        }

        private int find(SeparatedCharSequence topic) {
            matches = 0;
            root.findTopic(topic, this, null, null, null, MATCH_SINK, END_SINK);
            return matches;
        }
    }

    @Benchmark
    public int exactStringFind(TopicFinderState state) {
        return state.find(state.exactTopicSamples[state.nextIndex()]);
    }

    @Benchmark
    public int exactSeparatedFind(TopicFinderState state) {
        return state.find(state.exactSeqSamples[state.nextIndex()]);
    }

    @Benchmark
    public int missStringFind(TopicFinderState state) {
        return state.find(state.missTopicSamples[state.nextIndex()]);
    }

    @Benchmark
    public int missSeparatedFind(TopicFinderState state) {
        return state.find(state.missSeqSamples[state.nextIndex()]);
    }

    @Benchmark
    public int wildcardStringFind(TopicFinderState state) {
        return state.find(state.wildcardTopicSamples[state.nextIndex()]);
    }

    @Benchmark
    public int wildcardSeparatedFind(TopicFinderState state) {
        return state.find(state.wildcardSeqSamples[state.nextIndex()]);
    }
}
