package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.monitor.metrics.MetricStatistic;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 实例生命周期内的平均每秒速率；分子单位由 owner 的契约声明，如 bytes/s 或 sample/s。
 * 空单位表示尚未绑定业务量纲，不默认当成请求 QPS，也不转换数值尺度。
 *
 * @since 1.3.3
 * @see EwmaRate
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Gauge
@Statistic(MetricStatistic.rate)
@Expands(key = "metric", value = {
    @Attr(key = "aggregation", value = "mean"),
    @Attr(key = "perSeconds", value = "1", type = "long")
})
public @interface Rate {
    @AliasFor(annotation = Gauge.class, attribute = "unit")
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "";
}
