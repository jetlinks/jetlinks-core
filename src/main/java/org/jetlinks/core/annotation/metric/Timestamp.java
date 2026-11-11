package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.monitor.metrics.MetricSemantic;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/**
 * epoch 时间的 gauge，默认毫秒；不将读取时刻当成事件实际发生时刻。
 *
 * @since 1.3.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Metric(semantic = MetricSemantic.timestamp)
public @interface Timestamp {
    @AliasFor(annotation = Metric.class, attribute = "unit")
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "milliseconds";
}
