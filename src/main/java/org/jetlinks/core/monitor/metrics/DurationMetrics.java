package org.jetlinks.core.monitor.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.metric.Counter;
import org.jetlinks.core.annotation.metric.Duration;
import org.jetlinks.core.annotation.metric.Statistic;
import org.jetlinks.core.annotation.metric.Timestamp;
import org.jetlinks.core.annotation.metric.TotalDuration;

/**
 * 当前实例生命周期内的耗时摘要，读取不清零，不代表一小时分布或业务成功率。
 * count 为零时其余数值是无样本占位；峰值与时刻需要成对读取时使用 DurationSummary.snapshot()。
 *
 * @since 1.3.3
 * @see DurationSummary
 */
public interface DurationMetrics {
    @Counter
    @Statistic(MetricStatistic.count)
    @Schema(title = "耗时样本数", description = "已记录的耗时数量，零表示摘要尚无样本")
    long getCount();

    @TotalDuration
    @Statistic(value = MetricStatistic.total, sampleCount = "count")
    @Schema(title = "累计耗时", description = "实例创建以来累计的单调时钟耗时，单位秒")
    double getTotalDurationSeconds();

    @Duration(unit = "seconds")
    @Statistic(value = MetricStatistic.mean, sampleCount = "count")
    @Schema(title = "平均耗时", description = "累计耗时除以样本数，单位秒；无样本时零为占位")
    double getMeanDurationSeconds();

    @Duration(unit = "nanoseconds")
    @Statistic(value = MetricStatistic.last, sampleCount = "count")
    @Schema(title = "最近耗时", description = "最近一次记录的单调时钟耗时，单位纳秒")
    long getLastDurationNanos();

    @Duration(unit = "nanoseconds")
    @Statistic(value = MetricStatistic.maximum, sampleCount = "count")
    @Schema(title = "最大耗时", description = "实例创建以来的最大耗时，单位纳秒；不是窗口峰值")
    long getMaxDurationNanos();

    @Timestamp
    @Expands(key = "metric", value = {
        @Attr(key = "sampleCountProperty", value = "count"),
        @Attr(key = "valueProperty", value = "maxDurationNanos")
    })
    @Schema(title = "最大耗时发生时刻", description = "最大耗时首次出现的完成时刻，epoch毫秒")
    long getMaxAtMillis();
}
