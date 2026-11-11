package org.jetlinks.core.monitor.metrics;

/**
 * 指标值的含义。duration 为单次或最近耗时，timestamp 为 epoch 时间；均不表示 Timer。
 *
 * @since 1.3.3
 */
public enum MetricSemantic {
    value,
    duration,
    timestamp
}
