package org.jetlinks.core.benchmark;

import org.junit.Assume;
import org.junit.Test;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * TopicFinder JMH 的显式运行入口，默认测试流程不会执行耗时基准。
 */
public class TopicFinderJmhBenchmarkTest {

    @Test
    public void shouldRunTopicFinderBenchmark() throws Exception {
        Assume.assumeTrue("enable with -Dtopic.finder.benchmark=true",
                          Boolean.getBoolean("topic.finder.benchmark"));

        int forks = Integer.getInteger("topic.finder.forks", 3);
        int warmupIterations = Integer.getInteger("topic.finder.warmupIterations", 3);
        int measurementIterations = Integer.getInteger("topic.finder.measurementIterations", 5);
        int iterationSeconds = Integer.getInteger("topic.finder.iterationSeconds", 1);
        String result = System.getProperty(
            "topic.finder.result",
            "target/topic-finder-benchmark.json");
        String include = System.getProperty(
            "topic.finder.include",
            TopicFinderJmhBenchmark.class.getSimpleName());

        Options options = new OptionsBuilder()
            .include(include)
            .threads(1)
            .forks(forks)
            .warmupIterations(warmupIterations)
            .warmupTime(TimeValue.seconds(iterationSeconds))
            .measurementIterations(measurementIterations)
            .measurementTime(TimeValue.seconds(iterationSeconds))
            .addProfiler(GCProfiler.class)
            .result(result)
            .resultFormat(ResultFormatType.JSON)
            .build();

        new Runner(options).run();
    }
}
