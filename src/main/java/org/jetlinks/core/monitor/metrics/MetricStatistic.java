package org.jetlinks.core.monitor.metrics;

/**
 * 指标值的统计含义，与 counter/gauge 及耗时/时间戳语义正交。
 * 不决定业务测量边界或单位，也不自动执行聚合。
 *
 * @since 1.3.3
 */
public enum MetricStatistic {
    count,
    total,
    mean,
    last,
    maximum,
    rate,
    distribution
}
