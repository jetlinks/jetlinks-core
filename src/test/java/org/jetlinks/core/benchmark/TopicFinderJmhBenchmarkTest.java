package org.jetlinks.core.benchmark;

import org.junit.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class TopicFinderJmhBenchmarkTest {

    @Test
    public void shouldRunTopicFinderBenchmark() throws Exception {
        Options opt = new OptionsBuilder()
            .include(TopicFinderJmhBenchmark.class.getSimpleName())
            .warmupIterations(1)
            .warmupTime(TimeValue.seconds(1))
            .measurementIterations(1)
            .measurementTime(TimeValue.seconds(1))
            .forks(0)
            .build();

        new Runner(opt).run();
    }
}
