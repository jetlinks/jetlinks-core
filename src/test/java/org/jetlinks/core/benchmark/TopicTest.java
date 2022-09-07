package org.jetlinks.core.benchmark;

import lombok.SneakyThrows;
import org.jetlinks.core.topic.Topic;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1)
public class TopicTest {

    Topic<Integer> root = Topic.createRoot();


    @Setup
    public void init(){
        root.append("/a/b/c/d").subscribe(1);
        root.append("/a/b/c2/d").subscribe(2);
        root.append("/a/b/c3/d").subscribe(3);
        root.append("/a/b/*/d").subscribe(4);
        root.append("/a/**").subscribe(5);
    }

    @Benchmark
    @SneakyThrows
    public void testFindDirect(Blackhole blackhole) {
        root.findTopic("/a/b/c/d",
                       t -> blackhole.consume(t.getTotalSubscriber()),
                       () -> {

                       });
    }


    public static void main(String[] args) throws RunnerException {
        BenchmarkRunner.run(TopicTest.class);
    }

}
