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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 针对 TopicFinder 的精确查找与 wildcard 查找微基准.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 3)
@Measurement(iterations = 2, time = 5)
@Fork(value = 0, jvmArgs = {"-Xms2g", "-Xmx2g", "-XX:+UseG1GC"})
public class TopicFinderJmhBenchmark {

    private static final int TENANT_COUNT = 256;
    private static final int PRODUCT_COUNT = 128;
    private static final String EXACT_SUFFIX = "/message/property/report";
    private static final String WILDCARD_SUFFIX = "/message/*";

    @State(Scope.Benchmark)
    public static class TopicFinderState {
        Topic<Integer> root = Topic.createRoot();
        String[] exactTopicSamples = new String[1024];
        SeparatedCharSequence[] exactSeqSamples = new SeparatedCharSequence[1024];
        String[] wildcardTopicSamples = new String[1024];
        SeparatedCharSequence[] wildcardSeqSamples = new SeparatedCharSequence[1024];

        @Setup(Level.Trial)
        public void init() {
            for (int tenant = 0; tenant < TENANT_COUNT; tenant++) {
                root.append("/tenant/" + tenant + "/device/**").subscribe(tenant);
                for (int product = 0; product < PRODUCT_COUNT; product++) {
                    root.append("/tenant/" + tenant + "/device/" + product + EXACT_SUFFIX)
                        .subscribe(product);
                    root.append("/tenant/" + tenant + "/device/*" + EXACT_SUFFIX)
                        .subscribe(product);
                    root.append("/tenant/" + tenant + "/device/" + product + WILDCARD_SUFFIX)
                        .subscribe(product);
                }
            }
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < exactTopicSamples.length; i++) {
                int tenant = random.nextInt(TENANT_COUNT);
                int product = random.nextInt(PRODUCT_COUNT);
                String exact = "/tenant/" + tenant + "/device/" + product + EXACT_SUFFIX;
                String wildcard = "/tenant/" + tenant + "/device/" + product + "/message/*";
                exactTopicSamples[i] = exact;
                exactSeqSamples[i] = SharedPathString.of(exact);
                wildcardTopicSamples[i] = wildcard;
                wildcardSeqSamples[i] = SharedPathString.of(wildcard);
            }
        }
    }

    @Benchmark
    public void exactStringFind(TopicFinderState state, Blackhole blackhole) {
        String topic = state.exactTopicSamples[ThreadLocalRandom.current().nextInt(state.exactTopicSamples.length)];
        state.root.findTopic(topic,
                            node -> blackhole.consume(node.getSubscribers()),
                            () -> {
                            });
    }

    @Benchmark
    public void exactSeparatedFind(TopicFinderState state, Blackhole blackhole) {
        SeparatedCharSequence topic = state.exactSeqSamples[ThreadLocalRandom.current().nextInt(state.exactSeqSamples.length)];
        state.root.findTopic(topic,
                            node -> blackhole.consume(node.getSubscribers()),
                            () -> {
                            });
    }

    @Benchmark
    public void wildcardStringFind(TopicFinderState state, Blackhole blackhole) {
        String topic = state.wildcardTopicSamples[ThreadLocalRandom.current().nextInt(state.wildcardTopicSamples.length)];
        state.root.findTopic(topic,
                            node -> blackhole.consume(node.getSubscribers()),
                            () -> {
                            });
    }

    @Benchmark
    public void wildcardSeparatedFind(TopicFinderState state, Blackhole blackhole) {
        SeparatedCharSequence topic = state.wildcardSeqSamples[ThreadLocalRandom.current().nextInt(state.wildcardSeqSamples.length)];
        state.root.findTopic(topic,
                            node -> blackhole.consume(node.getSubscribers()),
                            () -> {
                            });
    }
}
