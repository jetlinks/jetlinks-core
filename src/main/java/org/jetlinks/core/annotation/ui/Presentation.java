package org.jetlinks.core.annotation.ui;

import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 给消费者提供展示提示。例如把产品摘要中的设备数量标为主要展示字段。
 * primary 只表示展示优先级，不表示唯一结果、默认查询或访问权限；具体布局仍由消费页面决定。
 * observedAt 与 Observation 注解使用同一采样时间映射，保留现有调用写法。
 *
 * @since 1.3.3
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands(key = MetadataSemantics.PRESENTATION_KEY)
public @interface Presentation {
    /** @return 是否优先展示此字段；允许多个字段同时声明，不表示只读取第一项 */
    @Attr(key = MetadataSemantics.PRIMARY_KEY)
    boolean primary() default false;

    /**
     * @return 相对当前字段所在记录的采样时间路径，例如 timestamp；空字符串表示未声明
     */
    @Attr(key = MetadataSemantics.OBSERVED_AT_KEY, ignoreEmpty = true)
    String observedAt() default "";
}
