package org.jetlinks.core.benchmark;

import org.jetlinks.core.monitor.metrics.DurationSummary;
import org.jetlinks.core.monitor.metrics.FixedHistogram;
import org.jetlinks.core.monitor.metrics.RateMeter;
import org.jetlinks.core.monitor.metrics.WindowMax;
import org.openjdk.jmh.annotations.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * 统计写入热路径及请求侧读取的成本。Local 使用独立 owner，Shared 用于多线程写入同一 owner。
 * 固定峰值与持续刷新峰值分别测量，避免只测量很少发生的锁路径或只测量理想路径。
 * 无网络/存储 I/O；通过 GCProfiler 观察逐次分配，不使用计时断言替代行为测试。
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class LowCostMetricsBenchmark {
    private static final FixedHistogram HISTOGRAM = new FixedHistogram(
        1_000_000L, 5_000_000L, 10_000_000L, 25_000_000L, 50_000_000L,
        100_000_000L, 250_000_000L, 500_000_000L, 1_000_000_000L,
        2_500_000_000L, 5_000_000_000L, 10_000_000_000L, 30_000_000_000L);

    @State(Scope.Thread)
    public static class Owner {
        private static final AtomicLongFieldUpdater<Owner> COUNT =
            AtomicLongFieldUpdater.newUpdater(Owner.class, "count");
        volatile long count;
        long cursor;
        final RateMeter rate = new RateMeter();
        final DurationSummary duration = new DurationSummary();
        final WindowMax peak = new WindowMax(Duration.ofHours(1));
        final WindowMax risingPeak = new WindowMax(Duration.ofHours(1));
        final long[] buckets = HISTOGRAM.createCounts();

        @Setup(Level.Trial)
        public void prepare() {
            duration.record(Long.MAX_VALUE, 1);
            peak.record(Long.MAX_VALUE, 1);
        }
    }

    @State(Scope.Thread)
    public static class Local extends Owner {
        volatile DurationSummary.Snapshot snapshot;
    }

    @State(Scope.Benchmark)
    public static class Shared extends Owner {
    }

    @Benchmark
    public long atomicCount(Local state) {
        return Owner.COUNT.incrementAndGet(state);
    }

    @Benchmark
    public void rateMark(Local state) {
        state.rate.mark();
    }

    @Benchmark
    public double rateRead(Local state) {
        return state.rate.getOneMinuteRate();
    }

    @Benchmark
    public void durationStablePeak(Local state) {
        state.duration.record(1_000_000, 1000);
    }

    @Benchmark
    public void durationWithClock(Local state) {
        state.duration.record(1_000_000);
    }

    @Benchmark
    public void peakUnchanged(Local state) {
        state.peak.record(1, 1000);
    }

    @Benchmark
    public void peakIncreasing(Local state) {
        state.risingPeak.record(++state.cursor, 1000);
    }

    @Benchmark
    public void histogramRecord(Local state) {
        HISTOGRAM.record(state.buckets, (state.cursor++ & 1023) * 1_000_000L);
    }

    @Benchmark
    public DurationSummary.Snapshot durationSnapshot(Local state) {
        // 模拟快照交给请求边界，防止仅丢弃返回值导致逃逸分析消除快照分配。
        state.snapshot = state.duration.snapshot();
        return state.snapshot;
    }

    @Benchmark
    public long sharedAtomicCount(Shared state) {
        return Owner.COUNT.incrementAndGet(state);
    }

    @Benchmark
    public void sharedRate(Shared state) {
        state.rate.mark();
    }

    @Benchmark
    public void sharedDuration(Shared state) {
        state.duration.record(1_000_000, 1000);
    }
}
