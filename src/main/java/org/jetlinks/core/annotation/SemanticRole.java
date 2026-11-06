package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.PropertySemanticRole;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 声明原字段的业务语义，字段 ID、单位、数据类型和权限仍由来源维护。
 * 开放词汇可继续使用原 Expands；此注解只提供内置角色的类型安全写法。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands
public @interface SemanticRole {
    @Attr(key = "semanticRole")
    PropertySemanticRole value();
}
