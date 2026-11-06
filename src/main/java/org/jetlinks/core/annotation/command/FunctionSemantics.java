package org.jetlinks.core.annotation.command;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.metadata.FunctionKind;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 命令类或处理方法的语义分类；不声明时保持未知。
 * query/action 不代替来源授权、幂等、重试或完成状态校验。
 *
 * @since 1.3.3
 * @see CommandHandler
 */
@Documented
@Target({TYPE, METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Expands
public @interface FunctionSemantics {
    @Attr(key = "functionKind")
    FunctionKind value();
}
