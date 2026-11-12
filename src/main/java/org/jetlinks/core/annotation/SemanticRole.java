package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.PropertySemanticRole;
import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 说明字段的用途。例如温度快照的 value 标为 result，timestamp 标为 observedAt；
 * 设备 ID 标为 subjectId，告警记录自身的 ID 标为 identity，二者不是同一种身份。
 * 字段仍通过原 Schema 等机制公开，值类型、单位和授权仍由来源提供。
 * 自定义角色可以通过原 Expands 声明；本注解只提供内置角色的枚举写法，不负责查询或建立对象关系。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands
public @interface SemanticRole {
    @Attr(key = MetadataSemantics.PROPERTY_ROLE_KEY)
    PropertySemanticRole value();
}
