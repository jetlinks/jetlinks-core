package org.jetlinks.core.benchmark;

import lombok.SneakyThrows;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.topic.Topic;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1)
public class TopicTest {

    Topic<Integer> root = Topic.createRoot();


    @Setup
    public void init() {
        //初始化订阅信息
        for (int i = 0; i < 10; i++) {
            Topic<Integer> product = root.append("/device/" + i);
            for (int i1 = 0; i1 < 10000; i1++) {
                Topic<Integer> device = product.append("/" + i1);
                device.append("/message/property/read");
                device.append("/message/property/write");
                device.append("/message/property/report");
                device.append("/message/function/invoke");
                device.append("/message/function/invoke/reply");
                device.append("/message/event");
                device.append("/online");
                device.append("/offline");
            }
            //订阅*
            Topic<Integer> device = product.append("/*");
            device.append("/message/property/read");
            device.append("/message/property/write");
            device.append("/message/property/report");
            device.append("/message/function/invoke");
            device.append("/message/function/invoke/reply");
            device.append("/message/event");
            device.append("/online");
            device.append("/offline");
        }
        //订阅*
        root.append("/device/*/*/message/**")
            .subscribe(1);

    }


    @Benchmark
    @SneakyThrows
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testFindAll(Blackhole blackhole) {

        root.findTopic("/device/" + ThreadLocalRandom.current().nextInt(1, 9) + "/*/message/property/read",
                       t -> blackhole.consume(t.getSubscribers()),
                       () -> {

                       });
    }

    static SharedPathString path = SharedPathString.of("/device/*/*/message/property/read");

    @Benchmark
    @SneakyThrows
    public void testFindDirectShared(Blackhole blackhole) {
        SeparatedCharSequence _path = path.replace(2, String.valueOf(ThreadLocalRandom.current().nextInt(1, 9)),
                                                   3, String.valueOf(ThreadLocalRandom.current().nextInt(1, 10000)));
        root.findTopic(_path,
                       t -> blackhole.consume(t.getSubscribers()),
                       () -> {
                           blackhole.consume("ok");
                       });
    }

    @Benchmark
    @SneakyThrows
    public void testFindDirect(Blackhole blackhole) {

        root.findTopic("/device/" + ThreadLocalRandom.current().nextInt(1, 9) +
                           "/" + ThreadLocalRandom.current()
                                                  .nextInt(1, 10000) + "/message/property/read",
                       t -> blackhole.consume(t.getSubscribers()),
                       () -> {
                           blackhole.consume("ok");
                       });
    }


    public static void main(String[] args) throws RunnerException {
        BenchmarkRunner.run(TopicTest.class);
    }

}
