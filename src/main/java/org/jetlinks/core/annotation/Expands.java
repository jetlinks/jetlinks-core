package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.Metadata;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * 使用注解来定义扩展信息,在实体属性,get方法参数上使用.
 * <p>
 * 建议使用注解继承的方式来定义通用的拓展信息.
 *
 * @author zhouhao
 * @see Metadata#getExpands()
 * @see org.jetlinks.core.annotation.ui.Selector
 * @since 1.2.3
 */
@Target({ANNOTATION_TYPE, FIELD, METHOD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(Expands.List.class)
public @interface Expands {

    /**
     * 扩展信息的key,用于区分不同的扩展信息,如果为空则平铺到expands中
     *
     * @return key
     * @see Metadata#getExpand(String)
     */
    String key() default "";

    /**
     * 扩展信息的值
     *
     * @return value
     */
    Attr[] value() default {};


    @Target({ANNOTATION_TYPE, FIELD, METHOD, PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    @interface List {
        Expands[] value();
    }
}