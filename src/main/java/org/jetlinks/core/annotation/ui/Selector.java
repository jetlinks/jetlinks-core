package org.jetlinks.core.annotation.ui;

import org.jetlinks.core.annotation.Expands;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 定义字段为一个选择器,用于选择指定类型的数据.如选择设备.
 *
 * @author zhouhao
 * @since 1.2.3
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Expands(key = Selector.KEY)
public @interface Selector {

    String KEY = "selector";

    /**
     * 选择器类型
     *
     * @return 类型
     */
    String type();

    /**
     * 是否多选
     *
     * @return 是否多选
     */
    boolean multiple() default false;
}
