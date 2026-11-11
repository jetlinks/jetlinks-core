package org.jetlinks.core.monitor.metrics;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * 可共享的不可变直方图边界，计数由 owner 的可选 long[] 持有。
 * 更新一次仅做桶定位和原子加一，不保存样本、owner 或历史；snapshot 只在读取时复制计数。
 * 同一边界/单位的快照可先合并再求分位，不能平均各实例的分位数。
 * 累计桶不表示最近一小时分布，分位结果为所在桶上界并受实际最大值限制。
 *
 * @since 1.3.3
 */
public final class FixedHistogram {
    private static final VarHandle COUNTS = MethodHandles.arrayElementVarHandle(long[].class);
    private final long[] upperBounds;

    /** 严格递增的包含式上界，最后自动增加溢出桶。配置按契约共享，不逐 owner 复制。 */
    public FixedHistogram(long... upperBounds) {
        this.upperBounds = upperBounds.clone();
        for (int i = 1; i < this.upperBounds.length; i++) {
            if (this.upperBounds[i] <= this.upperBounds[i - 1]) {
                throw new IllegalArgumentException("bucket bounds must be strictly increasing");
            }
        }
    }

    public int getBucketCount() {
        return upperBounds.length + 1;
    }

    /** 仅在显式启用分布统计时调用；普通累计无需该数组。 */
    public long[] createCounts() {
        return new long[getBucketCount()];
    }

    public int bucketIndex(long value) {
        int index = Arrays.binarySearch(upperBounds, value);
        return index < 0 ? -index - 1 : index;
    }

    /** counts 需至少有 getBucketCount() 项；允许 owner 在尾部存放自己的观测起点。 */
    public void record(long[] counts, long value) {
        COUNTS.getAndAdd(counts, bucketIndex(value), 1L);
    }

    /** 弱一致计数副本，不将 owner 附加的尾部数据混入桶。 */
    public long[] snapshot(long[] counts) {
        long[] result = createCounts();
        for (int i = 0; i < result.length; i++) {
            result[i] = (long) COUNTS.getVolatile(counts, i);
        }
        return result;
    }

    /**
     * 对已复制或已合并的桶估算分位；无样本、无效计数/参数返回 NaN。
     * maximum 与边界使用相同单位，须来自同一观测范围，不读取或修改 owner。
     */
    public double percentile(long[] counts, double quantile, double maximum) {
        if (counts == null || counts.length != getBucketCount() || !Double.isFinite(maximum)
            || !Double.isFinite(quantile) || quantile <= 0 || quantile > 1) {
            return Double.NaN;
        }
        long total = 0;
        for (long count : counts) {
            if (count < 0 || Long.MAX_VALUE - total < count) {
                return Double.NaN;
            }
            total += count;
        }
        if (total == 0) {
            return Double.NaN;
        }
        long rank = Math.min(total, (long) Math.ceil(total * quantile));
        long cumulative = 0;
        for (int i = 0; i < counts.length; i++) {
            cumulative += counts[i];
            if (cumulative >= rank) {
                return Math.min(maximum, i == upperBounds.length ? Long.MAX_VALUE : upperBounds[i]);
            }
        }
        return Double.NaN;
    }
}
