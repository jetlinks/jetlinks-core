package org.jetlinks.core.benchmark;

import org.jetlinks.core.Key;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.Observations;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.observation.RecorderSelection;
import org.jetlinks.core.monitor.recorder.AbstractActionRecorder;
import org.jetlinks.core.monitor.recorder.ActionRecord;
import org.jetlinks.core.monitor.recorder.ActionRecorder;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 同步单值业务上的观察开销；每次包含 Publisher 组装和订阅，基线为 Mono.defer。
 * Context 快照预先创建；参数区分空 Context 和已有多键的入口，不包含存储或网络 I/O。
 * 使用 JMH GCProfiler 比较每次分配，不在单测中断言易波动的耗时。
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ObservationsBenchmark {

    private static final Mono<Integer> RESULT = Mono.just(7);
    private static final Supplier<Mono<Integer>> OPERATION = () -> RESULT;
    private static final Function<Integer, Mono<Integer>> CALCULATE = input -> RESULT;
    private static final Key<String> INPUT = Key.stringKey("metric.input");
    private static final Key<String> OUTPUT = Key.stringKey("metric.output");
    private static final ObservationSpec<Integer, Integer> SPEC =
        ObservationSpec.<Integer, Integer>of("metric.calculate")
            .onInput((input, metadata) -> metadata.attribute(INPUT, "probe-a:1@1"))
            .onOutput((output, metadata) -> metadata.attribute(OUTPUT, "metric:room@1"));

    @Param({"0", "8"})
    public int contextEntries;

    private Context context;
    private Context disabled;
    private Context original;
    private Context detailed;
    private long completed;

    @Setup
    public void setup() {
        context = Context.empty();
        for (int i = 0; i < contextEntries; i++) {
            context = context.put("existing." + i, i);
        }
        Recorder recorder = new Recorder() {
            @Override
            public <E> ActionRecorder<E> action(CharSequence name) {
                return new AbstractActionRecorder<>(name) {
                    @Override
                    protected void handle(ActionRecord record) {
                        completed++;
                    }

                    @Override
                    public <T> ActionRecorder<T> child(CharSequence action) {
                        return ActionRecorder.noop();
                    }
                };
            }
        };
        disabled = context.put(RecorderProvider.class,
            (RecorderProvider) (action, ctx) -> RecorderSelection.disabled());
        original = context.put(Recorder.class, recorder);
        detailed = original.put(RecorderProvider.class,
            (RecorderProvider) (action, ctx) -> RecorderSelection.capture(Recorder.noop(), "policy@1"));
    }

    @Benchmark
    public Integer deferredBusiness() {
        return inContext(Mono.defer(OPERATION), context);
    }

    @Benchmark
    public Integer contextMissing() {
        return inContext(Observations.observeMono(SPEC, 7, CALCULATE), context);
    }

    @Benchmark
    public Integer contextMissingSimple() {
        return inContext(Observations.observeMono("metric.calculate", OPERATION), context);
    }

    @Benchmark
    public Integer explicitNoop() {
        return inContext(Observations.observeMono(Recorder.noop(), SPEC, 7, CALCULATE), context);
    }

    @Benchmark
    public Integer providerDisabled() {
        return inContext(Observations.observeMono(SPEC, 7, CALCULATE), disabled);
    }

    @Benchmark
    public long originalRecorder() {
        inContext(Observations.observeMono(SPEC, 7, CALCULATE), original);
        return completed;
    }

    @Benchmark
    public long detailedRecording() {
        inContext(Observations.observeMono(SPEC, 7, CALCULATE), detailed);
        return completed;
    }

    private static Integer inContext(Mono<Integer> operation, Context context) {
        return (context.isEmpty() ? operation : operation.contextWrite(context)).block();
    }
}
