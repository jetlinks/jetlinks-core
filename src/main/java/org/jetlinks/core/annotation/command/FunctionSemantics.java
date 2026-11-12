package org.jetlinks.core.annotation.command;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.metadata.FunctionKind;
import org.jetlinks.core.metadata.MetadataSemantics;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 说明命令的用途。例如查询设备属性快照标为 query，修改设备属性标为 action。
 * 未声明时用途保持未知，不能默认按只读功能执行；实际授权、重试和成功判定仍由原命令负责。
 *
 * @since 1.3.3
 * @see CommandHandler
 */
@Documented
@Target({TYPE, METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands
public @interface FunctionSemantics {
    @Attr(key = MetadataSemantics.FUNCTION_KIND_KEY)
    FunctionKind value();
}
