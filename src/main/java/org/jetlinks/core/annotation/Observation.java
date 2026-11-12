package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 告诉消费者：这个结果值对应哪个采样时间、哪个质量字段。
 * 例如设备温度值标注 {@code @Observation(observedAt = "timestamp")}，读取的是同一条快照的 timestamp；
 * 若来源另提供 valid 质量字段，可再声明 {@code quality = "valid"}。false 仍保留原值，不被当成缺失。
 * 此注解不声明结果类型或单位，也不替字段添加 SemanticRole；字段仍需通过 Schema 等原机制公开。
 * 线上使用已有 presentation 键，和 Presentation.observedAt 表达同一个映射。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.ObservationMapping
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands(key = MetadataSemantics.PRESENTATION_KEY)
public @interface Observation {
    /** @return 相对字段所在记录的时间路径，如 timestamp；long 时间值按 Unix 毫秒读取，空字符串表示不声明 */
    @Attr(key = MetadataSemantics.OBSERVED_AT_KEY, ignoreEmpty = true)
    String observedAt() default "";

    /** @return 相对字段所在记录的质量路径，如 valid；具体值由来源定义，空字符串表示不声明 */
    @Attr(key = MetadataSemantics.QUALITY_KEY, ignoreEmpty = true)
    String quality() default "";
}
