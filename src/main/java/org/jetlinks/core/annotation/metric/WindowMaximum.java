package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.monitor.metrics.MetricStatistic;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 固定时间分段峰值，关联同一快照的可用性和起止时间属性。
 * 不可用时不能将零占位视为真实值；窗口长度/对齐取自快照，不硬编码到类级元数据。
 *
 * @since 1.3.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Gauge
@Statistic(value = MetricStatistic.maximum, scope = "fixedWindow")
@Expands(key = "metric")
public @interface WindowMaximum {
    @Attr(key = "availableProperty")
    String available() default "available";

    @Attr(key = "windowStartProperty")
    String start() default "startMillis";

    @Attr(key = "windowEndProperty")
    String end() default "endMillis";

    @AliasFor(annotation = Gauge.class, attribute = "unit")
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "";
}
