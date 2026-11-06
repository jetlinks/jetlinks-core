package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.SemanticType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 声明模型可组合的数据角色，不自动注册资源、创建存储或授予调用能力。
 * 原元数据中的未知词汇仍由 codec 保留。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
@Documented
@Target({TYPE, FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands
public @interface SemanticTypes {
    @Attr(key = "semanticTypes")
    SemanticType[] value();
}
