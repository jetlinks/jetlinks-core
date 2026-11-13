package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 声明字段中的 ID 指向另一类对象，例如工单的 projectId 指向项目，并对应原“所属项目”关系。
 * 名称可来自同层已公开的 projectName；它只是来源快照，不证明目标存在或有权编辑。
 * 字段仍通过 Schema 等原机制公开，类型、取数和维护继续由来源负责。
 *
 * @since 1.3.3
 * @see org.jetlinks.core.metadata.PropertyReferenceMapping
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands(key = MetadataSemantics.REFERENCE_KEY)
public @interface PropertyReference {
    /** @return 目标对象类型，如 project；不使用显示名称或具体对象 ID */
    @Attr(key = MetadataSemantics.REFERENCE_TYPE_KEY)
    String targetType();

    /** @return 原关系 ID；未声明时只表示对象引用，不创建通用“关联到”关系 */
    @Attr(key = MetadataSemantics.REFERENCE_RELATION_KEY, ignoreEmpty = true)
    String relationId() default "";

    /** @return 相对字段所属对象的浏览方向；默认沿规范关系正向 */
    @Attr(key = MetadataSemantics.REFERENCE_REVERSE_KEY)
    boolean reverse() default false;

    /** @return 同层已公开的名称字段 ID；不接受脚本或任意查询表达式 */
    @Attr(key = MetadataSemantics.REFERENCE_LABEL_KEY, ignoreEmpty = true)
    String labelProperty() default "";
}
