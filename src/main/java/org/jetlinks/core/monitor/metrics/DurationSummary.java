package org.jetlinks.core.monitor.metrics;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * owner 持有的固定内存耗时摘要，不保存逐次样本或时间窗口。
 * 使用单调时钟之差记录非负纳秒，发生时刻为 epoch 毫秒，累计以 double 秒保存以避免 long 纳秒和溢出。
 * 并发累计不丢更新；多个字段为弱一致读取，snapshot 保证最大耗时与其发生时刻成对。
 * 空摘要通过 count == 0 识别；摘要不代表一小时分布或端到端业务成功。
 *
 * @since 1.3.3
 * @see DurationMetrics
 */
public final class DurationSummary implements DurationMetrics {
    private static final AtomicLongFieldUpdater<DurationSummary> COUNT =
        AtomicLongFieldUpdater.newUpdater(DurationSummary.class, "count");
    private static final AtomicLongFieldUpdater<DurationSummary> TOTAL =
        AtomicLongFieldUpdater.newUpdater(DurationSummary.class, "totalBits");

    private volatile long count;
    private volatile long totalBits;
    private volatile long lastNanos;
    private volatile long maxNanos = -1;
    private volatile long maxAtMillis;

    public void record(long nanos) {
        record(nanos, System.currentTimeMillis());
    }

    /** 允许调用方复用同次观测的时钟读取；occurredAtMillis 建议为操作完成时刻。 */
    public void record(long nanos, long occurredAtMillis) {
        if (nanos < 0) {
            throw new IllegalArgumentException("duration must be non-negative");
        }
        addNanos(TOTAL, this, nanos);
        lastNanos = nanos;
        if (nanos > maxNanos) {
            synchronized (this) {
                if (nanos > maxNanos) {
                    maxAtMillis = occurredAtMillis;
                    maxNanos = nanos;
                }
            }
        }
        COUNT.incrementAndGet(this);
    }

    /**
     * 对 owner 的 primitive long 字段按 double 位模式原子累计秒，无需逐字段 AtomicLong 对象。
     * 字段须初始为零，nanos 为非负单调时钟差；不清零、不保存 owner 引用。
     */
    public static <T> void addNanos(AtomicLongFieldUpdater<T> updater, T target, long nanos) {
        long previous;
        long next;
        do {
            previous = updater.get(target);
            next = Double.doubleToRawLongBits(Double.longBitsToDouble(previous) + nanos / 1_000_000_000D);
        } while (!updater.compareAndSet(target, previous, next));
    }

    public long getCount() {
        return count;
    }

    public double getTotalDurationSeconds() {
        return Double.longBitsToDouble(totalBits);
    }

    public double getMeanDurationSeconds() {
        long current = count;
        return current == 0 ? 0 : getTotalDurationSeconds() / current;
    }

    public long getLastDurationNanos() {
        return lastNanos;
    }

    public long getMaxDurationNanos() {
        return Math.max(0, maxNanos);
    }

    public long getMaxAtMillis() {
        return maxAtMillis;
    }

    /** 仅在读取边界分配，最大值和时刻成对，其余累计字段为弱一致读取。 */
    public synchronized Snapshot snapshot() {
        return new Snapshot(count, getTotalDurationSeconds(), lastNanos, Math.max(0, maxNanos), maxAtMillis);
    }

    /** 一次读取的耗时摘要；count 为零时没有样本，非窗口统计。 */
    public record Snapshot(long count, double totalDurationSeconds, long lastDurationNanos,
                           long maxDurationNanos, long maxAtMillis) implements DurationMetrics {
        @Override public long getCount() { return count; }
        @Override public double getTotalDurationSeconds() { return totalDurationSeconds; }
        @Override public double getMeanDurationSeconds() { return count == 0 ? 0 : totalDurationSeconds / count; }
        @Override public long getLastDurationNanos() { return lastDurationNanos; }
        @Override public long getMaxDurationNanos() { return maxDurationNanos; }
        @Override public long getMaxAtMillis() { return maxAtMillis; }
    }
}
