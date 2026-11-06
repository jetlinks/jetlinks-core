package org.jetlinks.core.annotation.ui;

import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.Attr;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 字段的通用展示声明；具体样式及业务格式由消费模块维护。
 *
 * @since 1.3.3
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands(key = "presentation")
public @interface Presentation {
    /** @return 是否为当前结果的主要展示字段 */
    boolean primary() default false;

    /**
     * @return 当前值对应的实际采样时间字段路径；空值表示未声明，不能以入库或查询时间替代
     */
    @Attr(key = "observedAt", ignoreEmpty = true)
    String observedAt() default "";
}
