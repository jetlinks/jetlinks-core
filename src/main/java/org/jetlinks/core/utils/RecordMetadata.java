package org.jetlinks.core.utils;

import com.google.common.base.Defaults;
import reactor.core.Exceptions;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Record组件字段及规范构造器元信息，供公共序列化适配器复用。
 * 读取原始组件值，不调用可能改变值的访问器；具名模块须开放对应包。
 */
public final class RecordMetadata {
    private static final ClassValue<RecordMetadata> CACHE = new ClassValue<>() {
        @Override
        protected RecordMetadata computeValue(Class<?> type) {
            return new RecordMetadata(type);
        }
    };

    private final Field[] fields;
    private final Constructor<?> constructor;
    private final Map<String, Integer> indexes;
    private final Object[] defaults;

    private RecordMetadata(Class<?> type) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException("Not a record: " + type.getName());
        }
        var components = type.getRecordComponents();
        fields = new Field[components.length];
        indexes = new HashMap<>(components.length);
        defaults = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        try {
            for (int index = 0; index < components.length; index++) {
                var component = components[index];
                fields[index] = type.getDeclaredField(component.getName());
                if (!fields[index].trySetAccessible()) {
                    throw new IllegalArgumentException("Record package is not open: " + type.getName());
                }
                types[index] = component.getType();
                defaults[index] = Defaults.defaultValue(types[index]);
                indexes.put(component.getName(), index);
            }
            constructor = type.getDeclaredConstructor(types);
            if (!constructor.trySetAccessible()) {
                throw new IllegalArgumentException("Record constructor is not accessible: " + type.getName());
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalArgumentException("Unable to inspect record " + type.getName(), error);
        }
    }

    public static RecordMetadata of(Class<?> type) {
        return CACHE.get(type);
    }

    public int size() {
        return fields.length;
    }

    public Object value(Object record, int index) throws IOException {
        try {
            return fields[index].get(record);
        } catch (IllegalAccessException error) {
            throw new IOException("Unable to read record component " + fields[index].getName(), error);
        }
    }

    public Object newInstance(Object[] values) throws IOException {
        try {
            return constructor.newInstance(values);
        } catch (InvocationTargetException error) {
            Exceptions.throwIfFatal(error.getCause());
            throw invalid("Record constructor rejected " + constructor.getDeclaringClass().getName(), error.getCause());
        } catch (ReflectiveOperationException | IllegalArgumentException error) {
            throw invalid("Unable to construct record " + constructor.getDeclaringClass().getName(), error);
        }
    }

    String name(int index) {
        return fields[index].getName();
    }

    Integer index(String name) {
        return indexes.get(name);
    }

    Object[] arguments() {
        return defaults.clone();
    }

    private static InvalidObjectException invalid(String message, Throwable cause) {
        InvalidObjectException error = new InvalidObjectException(message);
        error.initCause(cause);
        return error;
    }
}
