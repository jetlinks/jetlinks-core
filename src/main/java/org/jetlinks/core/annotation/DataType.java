package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.Metadata;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 使用注解来定义数据类型
 *
 * @author zhouhao
 * @see Metadata#getExpands()
 * @since 1.3.2
 */
@Target({ANNOTATION_TYPE, TYPE, FIELD, METHOD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DataType {

    Class<?> value();

    Class<?>[] generics() default {};

}