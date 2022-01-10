package org.jetlinks.core.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.metadata.types.*;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MetadataParser {

    public static PropertyMetadata withField(Field field, ResolvableType type) {
        Schema schema = field.getAnnotation(Schema.class);
        String id = field.getName();
        String name = schema == null ? field.getName() : schema.description();

        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setId(id);
        metadata.setName(name);
        metadata.setValueType(withType(type));

        return metadata;

    }

    public static DataType withType(ResolvableType type) {
        Class<?> clazz = type.toClass();
        if (clazz == Object.class) {
            return null;
        }
        if (List.class.isAssignableFrom(clazz)) {
            ArrayType arrayType = new ArrayType();
            arrayType.setElementType(withType(type.getGeneric(0)));
            return arrayType;
        }
        if (clazz.isArray()) {
            ArrayType arrayType = new ArrayType();
            arrayType.setElementType(withType(ResolvableType.forType(clazz.getComponentType())));
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

        ReflectionUtils.doWithFields(type.toClass(), field -> {
            Schema schema = field.getAnnotation(Schema.class);
            if (schema != null && !schema.hidden()) {
                objectType.addPropertyMetadata(withField(field, ResolvableType.forField(field, type)));
            }
        });
        return objectType;
    }

}
