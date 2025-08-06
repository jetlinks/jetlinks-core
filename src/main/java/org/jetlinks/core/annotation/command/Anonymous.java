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

/**
 * 标记命令允许匿名访问,在通过接口访问命令时,默认会进行权限控制.
 * <p>
 * 通过此注解标记命令不需要进行权限控制,通常用于第三方回调等场景使用.
 * <p>
 * 注意: 请实现命令时处理好权限校验等逻辑.
 *
 * @author zhouhao
 * @since 1.2.5
 */
@Target({TYPE, METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Expands
@Attr(key = CommandConstant.EXPANDS_ANONYMOUS, value = "true", type = BooleanType.ID)
public @interface Anonymous {

}
