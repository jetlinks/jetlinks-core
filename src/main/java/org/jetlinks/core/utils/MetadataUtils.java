package org.jetlinks.core.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.Metadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.things.ThingsConfigKeys;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import reactor.util.function.Tuples;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 物模型工具类
 *
 * @author zhouhao
 * @since 1.2.2
 */
public class MetadataUtils {

    /**
     * 解析物模型中的国际化消息,通过在物模型的{@link Metadata#getExpand(String)}中定义国际化信息.
     * <pre>{@code
     * {
     *     "id":"",
     *     "name:"",
     *     "expands":{
     *         "i18nMessages":{
     *             "name":{"zh":"名称","en":"name"},
     *             "description":{"zh":"描述","en":"description"}
     *         }
     *     }
     * }
     *
     * }</pre>
     *
     * @param locale     语言地区
     * @param metadata   物模型
     * @param key        消息key
     * @param defaultMsg 默认消息
     * @return 国际化消息
     * @see ThingsConfigKeys#i18nMessages
     */
    public static String resolveI18nMessage(Locale locale,
                                            Metadata metadata,
                                            String key,
                                            String defaultMsg) {
        return resolveI18nMessage(
            locale,
            metadata.getExpand(ThingsConfigKeys.i18nMessages).orElse(null),
            key,
            defaultMsg
        );
    }


    /**
     * 解析国际化消息,通过在物模型的{@link Metadata#getExpand(String)}中定义国际化信息.
     * <pre>{@code
     * {
     *    "name":{"zh":"名称","en":"name"},
     *    "description":{"zh":"描述","en":"description"}
     * }
     * }</pre>
     *
     * @param locale     语言地区
     * @param source     源数据
     * @param key        消息key
     * @param defaultMsg 默认消息
     * @return 国际化消息
     * @see ThingsConfigKeys#i18nMessages
     */
    public static String resolveI18nMessage(Locale locale,
                                            Map<String, Map<String, String>> source,
                                            String key,
                                            String defaultMsg) {
        if (MapUtils.isEmpty(source)) {
            return defaultMsg;
        }
        Map<String, String> i18n = source.get(key);
        if (MapUtils.isEmpty(i18n)) {
            return defaultMsg;
        }

        String msg = i18n.get(locale.toString());
        if (msg != null) {
            return msg;
        }

        msg = i18n.get(locale.getLanguage());
        if (msg != null) {
            return msg;
        }

        return defaultMsg;
    }


    /**
     * 根据类字段解析属性元数据
     *
     * @param field 字段
     * @param type  字段类型
     * @return 属性元数据
     */
    public static PropertyMetadata parseProperty(Field field, ResolvableType type) {
        return MetadataParser.withField(field, type);
    }

    /**
     * 根据java类型解析物模型类型
     *
     * @param type java类型
     * @return 物模型类型
     */
    public static DataType parseType(ResolvableType type) {
        return MetadataParser.withType(type);
    }

    static class MetadataParser {


        Set<Object> distinct = new HashSet<>();

        public static PropertyMetadata withField(Field field, ResolvableType type) {
            return new MetadataParser().withField0(null, field, type);
        }

        public static DataType withType(ResolvableType type) {
            return new MetadataParser().withType0(null, type);
        }


        private PropertyMetadata withField0(Object owner, Field field, ResolvableType type) {

            Schema schema = this.getSchema(type.toClass(), field);
            String id = field.getName();

            SimplePropertyMetadata metadata = new SimplePropertyMetadata();
            metadata.setId(id);
            metadata.setName(id);
            metadata.setValueType(withType0(field, type));

            if (null != schema) {
                if (StringUtils.hasText(schema.description())) {
                    metadata.setDescription(schema.description());
                    metadata.setName(schema.description());
                }

                if (StringUtils.hasText(schema.title())) {
                    metadata.setName(schema.title());
                }

            }
            return metadata;

        }

        private DataType withType0(Object owner, ResolvableType type) {
            Class<?> clazz = type.toClass();
            if (clazz == Object.class) {
                return null;
            }
            if (Publisher.class.isAssignableFrom(clazz)) {
                clazz = type.getGeneric(0).toClass();
            }
            if (List.class.isAssignableFrom(clazz)) {
                ArrayType arrayType = new ArrayType();
                arrayType.setElementType(withType0(owner, type.getGeneric(0)));
                return arrayType;
            }
            if (clazz.isArray()) {
                ArrayType arrayType = new ArrayType();
                arrayType.setElementType(withType0(owner, ResolvableType.forType(clazz.getComponentType())));
                return arrayType;
            }
            if (Map.class.isAssignableFrom(clazz)) {
                return new ObjectType();
            }
            if (clazz == String.class || clazz == Character.class) {
                return new StringType();
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new IntType().max(Byte.MAX_VALUE);
            }
            if (clazz == short.class || clazz == Short.class) {
                return new IntType().max(Short.MAX_VALUE).min(0);
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new IntType();
            }
            if (clazz == long.class || clazz == Long.class) {
                return new LongType();
            }
            if (clazz == float.class || clazz == Float.class) {
                return new FloatType();
            }
            if (clazz == double.class || clazz == Double.class) {
                return new DoubleType();
            }
            if (clazz == Date.class || clazz == LocalDateTime.class) {
                return new DateTimeType();
            }
            if (clazz == Boolean.class || clazz == boolean.class) {
                return new BooleanType();
            }
            if (clazz.isEnum()) {
                EnumType enumType = new EnumType();
                for (Object constant : clazz.getEnumConstants()) {
                    if (constant instanceof EnumDict) {
                        EnumDict<?> dict = ((EnumDict<?>) constant);
                        enumType.addElement(EnumType.Element.of(String.valueOf(dict.getValue()), dict.getText()));
                    } else {
                        Enum<?> dict = ((Enum<?>) constant);
                        enumType.addElement(EnumType.Element.of(dict.name(), dict.name()));
                    }
                }
                return enumType;
            }

            ObjectType objectType = new ObjectType();
            Class<?> fClass = clazz;
            ReflectionUtils.doWithFields(fClass, field -> {
                if (owner != null && !distinct.add(Tuples.of(owner, field))) {
                    objectType.addPropertyMetadata(withField0(fClass, field, ResolvableType.forClass(Map.class)));
                    return;
                }
                Schema schema = getSchema(fClass, field);
                if (schema != null && !schema.hidden()) {
                    objectType.addPropertyMetadata(withField0(fClass, field, ResolvableType.forField(field, type)));
                }
            });
            return objectType;
        }

        private Schema getSchema(Class<?> owner, Field field) {
            String name = field.getName();
            try {
                PropertyDescriptor descriptor = new PropertyDescriptor(name, owner);
                Method method = descriptor.getReadMethod();
                if (method != null) {
                    Schema schema = AnnotatedElementUtils.getMergedAnnotation(field, Schema.class);
                    if (schema != null) {
                        return schema;
                    }
                }
            } catch (IntrospectionException ignore) {

            }
            return AnnotatedElementUtils.getMergedAnnotation(field, Schema.class);
        }
    }
}
