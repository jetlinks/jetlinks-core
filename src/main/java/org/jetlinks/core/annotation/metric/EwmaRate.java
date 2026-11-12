package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.monitor.metrics.MetricStatistic;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 指数加权每秒速率；value 为衰减时间常数，不是精确滚动平均窗口。
 * tickSeconds 描述实现的惰性推进节拍，不要求后台采样线程。
 *
 * @since 1.3.3
 * @see Rate
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Gauge
@Statistic(value = MetricStatistic.rate, scope = "exponential")
@Expands(key = "metric", value = {
    @Attr(key = "aggregation", value = "ewma"),
    @Attr(key = "perSeconds", value = "1", type = "long")
})
public @interface EwmaRate {
    @Attr(key = "timeConstantSeconds")
    long value();

    @Attr(key = "tickSeconds")
    long tickSeconds();

    @AliasFor(annotation = Gauge.class, attribute = "unit")
    @Attr(key = "unit", ignoreEmpty = true)
    String unit() default "";
}
