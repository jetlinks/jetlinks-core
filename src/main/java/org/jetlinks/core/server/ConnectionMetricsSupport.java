package org.jetlinks.core.server;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/** Stateless numerical support for optional connection observations. No instances or per-connection counters are held here. */
public final class ConnectionMetricsSupport {
    private ConnectionMetricsSupport() { }

    /** 固定确认耗时桶数。各连接使用同一边界，跨连接先合并桶计数再求分位。 */
    public static final int ACK_DURATION_BUCKET_COUNT = 14;

    private static final VarHandle ACK_BUCKET_ACCESS = MethodHandles.arrayElementVarHandle(long[].class);

    /** Allocate only when explicitly enabled. The final slot records this distribution's start time. */
    public static long[] createAckDurationHistogram() {
        long[] histogram = new long[ACK_DURATION_BUCKET_COUNT + 1];
        histogram[ACK_DURATION_BUCKET_COUNT] = System.currentTimeMillis();
        return histogram;
    }

    /** Add one successful confirmation without allocating or blocking. Disabled histograms are a no-op. */
    public static void recordAckDuration(long[] histogram, long nanos) {
        if (histogram != null) ACK_BUCKET_ACCESS.getAndAdd(histogram, ackDurationBucket(nanos), 1L);
    }

    public static long[] ackDurationHistogramSnapshot(long[] histogram) {
        if (histogram == null) return null;
        long[] counts = new long[ACK_DURATION_BUCKET_COUNT];
        for (int i = 0; i < counts.length; i++) counts[i] = (long) ACK_BUCKET_ACCESS.getVolatile(histogram, i);
        return counts;
    }

    /** 将单调时钟纳秒差映射到固定桶；不持有逐包历史或每连接直方图对象。 */
    public static int ackDurationBucket(long nanos) {
        for (int bucket = 0; bucket < ACK_DURATION_BUCKET_COUNT - 1; bucket++) {
            if (nanos <= ackDurationBucketUpperNanos(bucket)) return bucket;
        }
        return ACK_DURATION_BUCKET_COUNT - 1;
    }

    /** Lock-free addition to a double represented by a primitive long field; no per-counter object or nanosecond-total overflow. */
    public static <T> void addAckDuration(AtomicLongFieldUpdater<T> updater, T target, long nanos) {
        long previous;
        long next;
        do {
            previous = updater.get(target);
            next = Double.doubleToRawLongBits(Double.longBitsToDouble(previous) + nanos / 1_000_000_000D);
        } while (!updater.compareAndSet(target, previous, next));
    }

    private static long ackDurationBucketUpperNanos(int bucket) {
        return switch (bucket) {
            case 0 -> 1000000L;
            case 1 -> 5000000L;
            case 2 -> 10000000L;
            case 3 -> 25000000L;
            case 4 -> 50000000L;
            case 5 -> 100000000L;
            case 6 -> 250000000L;
            case 7 -> 500000000L;
            case 8 -> 1000000000L;
            case 9 -> 2500000000L;
            case 10 -> 5000000000L;
            case 11 -> 10000000000L;
            case 12 -> 30000000000L;
            default -> Long.MAX_VALUE;
        };
    }

    /** 返回所落桶的上界估计，并限制为实际观测最大值；不能将各连接分位数直接求平均。 */
    public static Double ackDurationPercentile(long[] buckets, double percentile, Double maximum) {
        if (buckets == null || buckets.length != ACK_DURATION_BUCKET_COUNT || maximum == null
            || !Double.isFinite(maximum) || maximum < 0 || !Double.isFinite(percentile) || percentile <= 0 || percentile > 1) return null;
        long total = 0;
        for (long count : buckets) {
            if (count < 0 || Long.MAX_VALUE - total < count) return null;
            total += count;
        }
        if (total == 0) return null;
        long rank = (long) Math.ceil(total * percentile);
        long count = 0;
        for (int bucket = 0; bucket < buckets.length; bucket++) {
            count += buckets[bucket];
            if (count >= rank) return Math.min(maximum, ackDurationBucketUpperNanos(bucket) / 1_000_000_000D);
        }
        return null;
    }
}
