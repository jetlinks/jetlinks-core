package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.monitor.metrics.MetricKind;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/**
 * 生命周期内单调累计值；读取不清零，重启边界由运行实例身份区分。
 *
 * @since 1.3.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Metric(kind = MetricKind.counter)
public @interface Counter {
    @AliasFor(annotation = Metric.class, attribute = "unit")
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "count";
}
