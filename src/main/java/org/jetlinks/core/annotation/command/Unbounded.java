package org.jetlinks.core.annotation.command;

import org.jetlinks.core.annotation.Attr;
import org.jetlinks.core.annotation.Expands;
import org.jetlinks.core.command.CommandConstant;
import org.jetlinks.core.metadata.types.BooleanType;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

/**
 * 标记命令为一个无界命令，响应流不会主动中断.
 *
 * @author zhouhao
 * @since 1.2.4
 */
@Target({TYPE, METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Expands
@Attr(key = CommandConstant.EXPANDS_UNBOUNDED, value = "true", type = BooleanType.ID)
public @interface Unbounded {
}
