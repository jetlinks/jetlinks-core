package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 引用字段、模型或功能所表达的领域概念，例如温度字段引用同一稳定的温度概念 ID。
 * 概念引用与字段 ID、semanticRole、单位和读取路径分开；不会自动换算、合并数据或授予权限。
 * 通过原 Expands 解析，不解析外部 IRI，也不要求业务依赖术语服务。
 *
 * @since 1.3.3
 * @see MetadataSemantics#withSemanticId
 */
@Documented
@Target({TYPE, FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands
public @interface SemanticId {
    @Attr(key = MetadataSemantics.SEMANTIC_ID_KEY)
    String value();
}
