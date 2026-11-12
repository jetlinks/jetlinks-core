package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.monitor.metrics.MetricKind;
import org.jetlinks.core.monitor.metrics.MetricSemantic;
import org.jetlinks.core.monitor.metrics.MetricStatistic;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 生命周期内累计耗时，读取不清零；默认秒，和单次耗时的 gauge 区分。
 *
 * @since 1.3.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Metric(kind = MetricKind.counter, semantic = MetricSemantic.duration)
@Statistic(MetricStatistic.total)
public @interface TotalDuration {
    @AliasFor(annotation = Metric.class, attribute = "unit")
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "seconds";
}
