package org.jetlinks.core.monitor.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.LongSupplier;

/**
 * 固定内存的累计与每秒速率统计，适用于次数、样本数或字节数。
 * owner 持有实例；更新和读取时惰性推进五秒节拍，不创建定时器或保存历史。
 * 1/5/15 分钟值是 EWMA，首次节拍前为零，并非对应窗口的精确算术平均。
 * 并发更新不丢计数，节拍边界的归属和多个 getter 为弱一致读取。
 *
 * @since 1.3.3
 * @see RateMetrics
 */
public final class RateMeter implements RateMetrics {
    private static final long INTERVAL = TimeUnit.SECONDS.toNanos(5);
    private static final double ONE_ALPHA = -Math.expm1(-5D / 60);
    private static final double FIVE_ALPHA = -Math.expm1(-5D / 300);
    private static final double FIFTEEN_ALPHA = -Math.expm1(-5D / 900);
    private static final LongSupplier CLOCK = System::nanoTime;
    private static final AtomicLongFieldUpdater<RateMeter> COUNT =
        AtomicLongFieldUpdater.newUpdater(RateMeter.class, "count");

    private final LongSupplier clock;
    private final long startedAt;
    private volatile long count;
    private volatile long lastTick;
    private long tickCount;
    private boolean initialized;
    private volatile double oneMinuteRate;
    private volatile double fiveMinuteRate;
    private volatile double fifteenMinuteRate;

    public RateMeter() {
        this(CLOCK);
    }

    RateMeter(LongSupplier clock) {
        this.clock = clock;
        this.startedAt = this.lastTick = clock.getAsLong();
    }

    /** 记录一次事件，不分配样本或计数对象。 */
    public void mark() {
        mark(1);
    }

    /** 批量记录非负增量；同一实例的单位须保持一致。 */
    public void mark(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        tick(clock.getAsLong());
        COUNT.addAndGet(this, amount);
    }

    public long getCount() {
        return count;
    }

    /** @return 实例创建以来的平均每秒速率，尚无时间跨度时为零 */
    public double getMeanRate() {
        long elapsed = clock.getAsLong() - startedAt;
        return elapsed <= 0 ? 0 : count / (elapsed / 1_000_000_000D);
    }

    public double getOneMinuteRate() {
        tick(clock.getAsLong());
        return oneMinuteRate;
    }

    public double getFiveMinuteRate() {
        tick(clock.getAsLong());
        return fiveMinuteRate;
    }

    public double getFifteenMinuteRate() {
        tick(clock.getAsLong());
        return fifteenMinuteRate;
    }

    private void tick(long now) {
        if (now - lastTick < INTERVAL) {
            return;
        }
        synchronized (this) {
            long ticks = (now - lastTick) / INTERVAL;
            if (ticks < 1) {
                return;
            }
            long current = count;
            double rate = (current - tickCount) / 5D;
            tickCount = current;
            if (initialized) {
                oneMinuteRate += ONE_ALPHA * (rate - oneMinuteRate);
                fiveMinuteRate += FIVE_ALPHA * (rate - fiveMinuteRate);
                fifteenMinuteRate += FIFTEEN_ALPHA * (rate - fifteenMinuteRate);
            } else {
                oneMinuteRate = fiveMinuteRate = fifteenMinuteRate = rate;
                initialized = true;
            }
            // 跳过的空闲节拍直接指数衰减，不按空闲时长循环。
            if (ticks > 1) {
                double idleSeconds = (ticks - 1) * 5D;
                oneMinuteRate *= Math.exp(-idleSeconds / 60);
                fiveMinuteRate *= Math.exp(-idleSeconds / 300);
                fifteenMinuteRate *= Math.exp(-idleSeconds / 900);
            }
            lastTick += ticks * INTERVAL;
        }
    }
}
