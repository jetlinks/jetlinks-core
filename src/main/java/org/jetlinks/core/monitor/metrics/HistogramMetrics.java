package org.jetlinks.core.monitor.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetlinks.core.annotation.metric.Gauge;
import org.jetlinks.core.annotation.metric.HistogramBuckets;

/**
 * 一次直方图分布快照。counts[i] 对应第 i 个包含式上界，末尾溢出桶没有有限上界。
 * 桶内计数生命周期累计、桶间独立；边界量纲由业务声明。数组读取返回副本，不泄露共享边界。
 *
 * @since 1.3.3
 * @see FixedHistogram.Snapshot
 */
public interface HistogramMetrics {
    @Gauge
    @Schema(title = "桶上界", description = "严格递增的包含式有限上界；具体量纲与被观测值一致")
    long[] getUpperBounds();

    @HistogramBuckets
    @Schema(title = "桶样本数", description = "各桶独立累计计数，末尾为溢出桶；全部为零表示没有样本")
    long[] getBucketCounts();
}
