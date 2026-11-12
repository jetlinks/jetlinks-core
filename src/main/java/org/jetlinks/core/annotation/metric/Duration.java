package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.monitor.metrics.MetricSemantic;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/**
 * 以时长表达的 gauge，默认毫秒；单次、平均或最大等统计方式可由 Statistic 声明。
 * 不自动提供样本窗口、分位数或 Timer 聚合。
 *
 * @since 1.3.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Metric(semantic = MetricSemantic.duration)
public @interface Duration {
    @AliasFor(annotation = Metric.class, attribute = "unit")
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "milliseconds";
}
