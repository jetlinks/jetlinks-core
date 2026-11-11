package org.jetlinks.core.monitor.metrics;

import java.time.Duration;

/**
 * 固定时间分段内的最大值及发生时刻，适用于本小时/今日峰值，不代表滚动窗口最大值。
 * owner 持有实例；只保留当前分段，空闲时无需定时清理，读取过期分段返回 unavailable。
 * 未刷新峰值时只读取 primitive 字段，新分段和新峰值才加锁，不逐条分配对象。
 * 使用本地 epoch 毫秒，忽略迟到的旧分段记录；相同最大值保留首次记录的时刻。
 *
 * @since 1.3.3
 */
public final class WindowMax {
    private final long periodMillis;
    private final long offsetMillis;
    private volatile boolean initialized;
    private volatile long window;
    private volatile long maximum;
    private long occurredAtMillis;

    /** 分段从 UTC epoch 对齐；日分段默认 UTC 午夜。 */
    public WindowMax(Duration period) {
        this(period, Duration.ZERO);
    }

    /**
     * 固定时长与相对 epoch 的偏移。UTC+8 的午夜可用 period=1天、offset=-8小时。
     * 固定时长不处理夏令时导致的23/25小时自然日。
     */
    public WindowMax(Duration period, Duration offset) {
        this.periodMillis = period.toMillis();
        if (periodMillis <= 0) {
            throw new IllegalArgumentException("period must be at least one millisecond");
        }
        this.offsetMillis = Math.floorMod(offset.toMillis(), periodMillis);
    }

    public void record(long value) {
        record(value, System.currentTimeMillis());
    }

    /** 记录已测量的值及其完成时刻，允许复用调用方已读取的时钟。 */
    public void record(long value, long atMillis) {
        long current = windowOf(atMillis);
        if (initialized && (current < window || current == window && value <= maximum)) {
            return;
        }
        synchronized (this) {
            if (!initialized || current > window) {
                maximum = value;
                occurredAtMillis = atMillis;
                window = current;
                initialized = true;
            } else if (current == window && value > maximum) {
                maximum = value;
                occurredAtMillis = atMillis;
            }
        }
    }

    public Snapshot snapshot() {
        return snapshot(System.currentTimeMillis());
    }

    /** 查询指定时刻所在分段；不复活已经被新分段替换的历史。 */
    public synchronized Snapshot snapshot(long nowMillis) {
        long current = windowOf(nowMillis);
        long start = current * periodMillis + offsetMillis;
        boolean available = initialized && current == window;
        return new Snapshot(start, start + periodMillis, available,
                            available ? maximum : 0, available ? occurredAtMillis : 0);
    }

    private long windowOf(long atMillis) {
        return Math.floorDiv(atMillis - offsetMillis, periodMillis);
    }

    /** 不可用分段没有样本，不能将其零占位当成真实峰值。 */
    public record Snapshot(long startMillis, long endMillis, boolean available,
                           long maximum, long occurredAtMillis) {
    }
}
