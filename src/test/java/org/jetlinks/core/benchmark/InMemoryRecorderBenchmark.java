package org.jetlinks.core.benchmark;

import org.jetlinks.core.monitor.recorder.AbstractActionRecorder;
import org.jetlinks.core.monitor.recorder.ActionRecord;
import org.jetlinks.core.monitor.recorder.ActionRecorder;
import org.jetlinks.core.monitor.recorder.InMemoryRecorder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import reactor.util.context.Context;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 接收端本身的创建、两字段写入和提取开销，不含 Reactor 包装、网络或存储。 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class InMemoryRecorderBenchmark {
    private final Supplier<InMemoryRecorder> factory = InMemoryRecorder.factory(Set.of("device"), Set.of("output"));
    private final Supplier<InMemoryRecorder> filtered = InMemoryRecorder.factory(Set.of(), Set.of());

    @Benchmark
    public InMemoryRecorder.Snapshot selectedSnapshot() {
        try (InMemoryRecorder recorder = factory.get()) {
            recorder.action("calculate").start(Context.empty()).tag("device", "probe")
                .attribute("output", 42).complete();
            return recorder.snapshot();
        }
    }

    @Benchmark
    public InMemoryRecorder.Snapshot filteredSnapshot() {
        try (InMemoryRecorder recorder = filtered.get()) {
            recorder.action("calculate").start(Context.empty()).tag("device", "probe")
                .attribute("output", 42).complete();
            return recorder.snapshot();
        }
    }

    @Benchmark
    public ActionRecord fullActionRecord() {
        FullAction action = new FullAction();
        action.start(Context.empty()).tag("device", "probe").attribute("output", 42).complete();
        return action.result;
    }

    private static final class FullAction extends AbstractActionRecorder<Object> {
        private ActionRecord result;
        private FullAction() { super("calculate"); }
        @Override protected void handle(ActionRecord record) { result = record; }
        @Override public <T> ActionRecorder<T> child(CharSequence action) { return ActionRecorder.noop(); }
    }
}
