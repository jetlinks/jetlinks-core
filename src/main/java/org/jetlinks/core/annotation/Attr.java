package org.jetlinks.core.annotation;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ANNOTATION_TYPE, FIELD, METHOD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(Attr.List.class)
public @interface Attr {

    String key();

    String value() default "";

    @Target({ANNOTATION_TYPE, FIELD, METHOD, PARAMETER})
    @Retention(RUNTIME)
    @Inherited
    @Documented
    @interface List {
        Attr[] value();
    }
}