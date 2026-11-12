package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.monitor.metrics.MetricStatistic;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 生命周期累计的独立桶计数数组，关联同一快照的包含式上界数组，末尾有一个溢出桶。
 * 桶间不累计；外部导出器需要按目标协议转换，不直接当作 Prometheus 的 le 累计桶。
 *
 * @since 1.3.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Gauge(unit = "count")
@Statistic(MetricStatistic.distribution)
@Expands(key = "metric", value = {
    @Attr(key = "bucketMode", value = "independent"),
    @Attr(key = "boundary", value = "inclusiveUpper"),
    @Attr(key = "overflowBucket", value = "true", type = "boolean"),
    @Attr(key = "quantileEstimation", value = "bucketUpperBound")
})
public @interface HistogramBuckets {
    @Attr(key = "boundsProperty")
    String bounds() default "upperBounds";
}
