package org.jetlinks.core.server;

import org.jetlinks.core.monitor.metrics.DurationSummary;
import org.jetlinks.core.monitor.metrics.FixedHistogram;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/** Stateless numerical support for optional connection observations. No instances or per-connection counters are held here. */
public final class ConnectionMetricsSupport {
    private ConnectionMetricsSupport() { }

    /** 固定确认耗时桶数。各连接使用同一边界，跨连接先合并桶计数再求分位。 */
    public static final int ACK_DURATION_BUCKET_COUNT = 14;

    private static final FixedHistogram ACK_HISTOGRAM = new FixedHistogram(
        1_000_000L, 5_000_000L, 10_000_000L, 25_000_000L, 50_000_000L,
        100_000_000L, 250_000_000L, 500_000_000L, 1_000_000_000L,
        2_500_000_000L, 5_000_000_000L, 10_000_000_000L, 30_000_000_000L);

    /** Allocate only when explicitly enabled. The final slot records this distribution's start time. */
    public static long[] createAckDurationHistogram() {
        long[] histogram = new long[ACK_DURATION_BUCKET_COUNT + 1];
        histogram[ACK_DURATION_BUCKET_COUNT] = System.currentTimeMillis();
        return histogram;
    }

    /** Add one successful confirmation without allocating or blocking. Disabled histograms are a no-op. */
    public static void recordAckDuration(long[] histogram, long nanos) {
        if (histogram != null) ACK_HISTOGRAM.record(histogram, nanos);
    }

    public static long[] ackDurationHistogramSnapshot(long[] histogram) {
        if (histogram == null) return null;
        return ACK_HISTOGRAM.snapshot(histogram);
    }

    /** 将单调时钟纳秒差映射到固定桶；不持有逐包历史或每连接直方图对象。 */
    public static int ackDurationBucket(long nanos) {
        return ACK_HISTOGRAM.bucketIndex(nanos);
    }

    /** Lock-free addition to a double represented by a primitive long field; no per-counter object or nanosecond-total overflow. */
    public static <T> void addAckDuration(AtomicLongFieldUpdater<T> updater, T target, long nanos) {
        DurationSummary.addNanos(updater, target, nanos);
    }

    /** 返回所落桶的上界估计，并限制为实际观测最大值；不能将各连接分位数直接求平均。 */
    public static Double ackDurationPercentile(long[] buckets, double percentile, Double maximum) {
        if (maximum == null || !Double.isFinite(maximum) || maximum < 0) return null;
        double upperNanos = ACK_HISTOGRAM.percentile(buckets, percentile, Long.MAX_VALUE);
        return Double.isNaN(upperNanos) ? null : Math.min(maximum, upperNanos / 1_000_000_000D);
    }
}
