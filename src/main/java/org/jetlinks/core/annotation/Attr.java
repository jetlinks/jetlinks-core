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
     * 在组合注解的成员方法上使用时，将该成员值映射到此扩展键；
     * 枚举及枚举数组按稳定名称编码，不将 Java 类型写入元数据协议。
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

    /** @return 是否省略空字符串/数组等未声明值；false 和 0 仍为有效值 */
    boolean ignoreEmpty() default false;

    @Target({ANNOTATION_TYPE, FIELD, METHOD, PARAMETER})
    @Retention(RUNTIME)
    @Inherited
    @Documented
    @interface List {
        Attr[] value();
    }
}
