package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 标记这条记录描述谁。例如设备属性快照的 deviceId 指向被测设备，不是快照自身的记录 ID。
 * 与 SemanticRole.subjectId 表达同一主体用途，保留此注解供现有声明使用。
 * 对象类型由查询上下文提供；混合多个对象类型且 ID 相同时，不能只凭此字段确定主体。
 * 本注解不授予权限，也不自动把快照注册成新对象；记录自身的 ID 应另行标注 identity。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands(@Attr(key = MetadataSemantics.GRAPH_SUBJECT_KEY, value = "true", type = "boolean"))
public @interface GraphSubject {
}
