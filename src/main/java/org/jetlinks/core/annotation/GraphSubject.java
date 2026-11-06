package org.jetlinks.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 标记记录中被描述对象的身份字段，便于通用图谱投影关联查询结果与对象。
 * 仅声明语义，不授予访问权限，也不把汇总记录自身声明为业务对象。
 * 此字段不是记录自身的 identity；确实同时承担记录身份时须另行显式声明。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.MetadataSemantics
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands(@Attr(key = "graphSubject", value = "true", type = "boolean"))
public @interface GraphSubject {
}
