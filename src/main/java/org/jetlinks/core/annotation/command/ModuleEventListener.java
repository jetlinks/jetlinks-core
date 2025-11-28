package org.jetlinks.core.annotation.command;

import org.jetlinks.core.annotation.Expands;

import java.lang.annotation.*;

/**
 * 标记要监听哪些模块事件
 *
 * @author zhouhao
 * @since 1.3.2
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Expands(key = ModuleEventListener.EXPANDS_KEY)
public @interface ModuleEventListener {

    String EXPANDS_KEY = "eventListener";

    String ATTR_EVENTS = "events";

    /**
     * 定义监听的事件,事件标识由业务实现定义,通常使用对应的命令ID作为事件.
     */
    String[] events() default {};

}
