package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.monitor.metrics.MetricKind;
import org.jetlinks.core.monitor.metrics.MetricSemantic;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 度量语义的基础元注解，优先组合 Counter、Gauge 等常用注解。
 * 名称、说明及类型沿用 Schema 与 PropertyMetadata；不声明实例身份或采样值。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.monitor.metrics.MetricSet
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands(key = "metric")
public @interface Metric {
    @Attr(key = "kind")
    MetricKind kind() default MetricKind.gauge;

    /** @return 既有 ValueUnits 单位 ID；编译后归一到 valueType.unit，不改变数值尺度 */
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "";

    @Attr(key = "semantic")
    MetricSemantic semantic() default MetricSemantic.value;
}
