package org.jetlinks.core.monitor.metrics;

/**
 * 指标值的含义。duration 为耗时时长，timestamp 为 epoch 时间；均不表示 Timer。
 * 累计、平均或峰值等统计方式另由 kind / statistic 声明。
 *
 * @since 1.3.3
 */
public enum MetricSemantic {
    value,
    duration,
    timestamp
}
