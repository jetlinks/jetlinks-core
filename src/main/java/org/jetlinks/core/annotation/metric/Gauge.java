package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/**
 * 按需读取的当前值，不维护最后一次上报的副本。
 *
 * @since 1.3.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Metric
public @interface Gauge {
    @AliasFor(annotation = Metric.class, attribute = "unit")
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "";
}
