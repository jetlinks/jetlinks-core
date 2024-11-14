package org.jetlinks.core.annotation.ui;


import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.*;

/**
 * 表单字段注解,用于标记一个字段为表单字段.
 *
 * @author zhouhao
 * @see org.jetlinks.core.metadata.PropertyMetadata
 * @since 1.2.3
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface FormField {

    /**
     * @return 显示名称
     * @see Schema#title()
     */
    String label() default "";

    /**
     * @return 是否隐藏
     */
    boolean hidden() default false;

    /**
     * @return 是否忽略
     */
    boolean ignore() default false;


}
