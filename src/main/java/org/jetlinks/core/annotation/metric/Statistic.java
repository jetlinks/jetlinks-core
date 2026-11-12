package org.jetlinks.core.annotation.metric;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.monitor.metrics.MetricStatistic;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 补充指标的统计方式与范围，和 Counter、Duration 等组合使用。
 * 只声明元数据，不创建统计对象；sampleCount 指向同一契约的样本数属性，零表示没有样本。
 *
 * @since 1.3.3
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands(key = "metric")
public @interface Statistic {
    @Attr(key = "statistic")
    MetricStatistic value();

    /** @return 统计范围；内置使用 lifetime、exponential、fixedWindow，不隐含精确滚动窗口 */
    @Attr(key = "scope")
    String scope() default "lifetime";

    @Attr(key = "sampleCountProperty", ignoreEmpty = true)
    String sampleCount() default "";
}
