package org.jetlinks.core.annotation;

import org.jetlinks.core.metadata.DataType;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ANNOTATION_TYPE, FIELD, METHOD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(Attr.List.class)
public @interface Attr {

    /**
     * KEY
     *
     * @return key
     */
    String key();

    /**
     * 值
     *
     * @return value
     */
    String value() default "";

    /**
     * 类型
     *
     * @return 类型ID
     * @see DataType#getId()
     * @see org.jetlinks.core.metadata.types.BooleanType#ID
     * @see org.jetlinks.core.utils.MetadataUtils#parseAttrValue(Attr)
     */
    String type() default "";

    @Target({ANNOTATION_TYPE, FIELD, METHOD, PARAMETER})
    @Retention(RUNTIME)
    @Inherited
    @Documented
    @interface List {
        Attr[] value();
    }
}