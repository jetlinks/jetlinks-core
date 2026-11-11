package org.jetlinks.core.monitor.metrics;

/**
 * 指标的累计或瞬时语义，不根据 Java 数值类型推断速率或直方图。
 *
 * @since 1.3.3
 */
public enum MetricKind {
    counter,
    gauge
}
