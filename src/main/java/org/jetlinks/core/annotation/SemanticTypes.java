package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.SemanticType;
import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 说明整个模型描述什么，例如设备快照可同时标为 observation 和 result。
 * 这是记录整体的分类；value 是结果值、timestamp 是采样时间等字段用途由 SemanticRole 另行说明。
 * 声明分类不会自动注册对象、建立数据库表或授予调用权限，原模型仍保留自定义词。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
@Documented
@Target({TYPE, FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands
public @interface SemanticTypes {
    @Attr(key = MetadataSemantics.SEMANTIC_TYPES_KEY)
    SemanticType[] value();
}
