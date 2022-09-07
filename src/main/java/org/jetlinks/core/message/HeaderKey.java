package org.jetlinks.core.message;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public interface HeaderKey<T> {

    String getKey();

    T getDefaultValue();

    @SuppressWarnings("all")
    @Deprecated
    default Class<T> getType() {
        return getDefaultValue() == null ? (Class<T>) Object.class : (Class<T>) getDefaultValue().getClass();
    }

    default Type getValueType() {
        return getType();
    }

    static <T> HeaderKey<T> ofSupplier(String key, Supplier<T> defaultValue, Type type) {
        return new HeaderKey<T>() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public T getDefaultValue() {
                return defaultValue.get();
            }

            @Override
            public Type getValueType() {
                return type;
            }
        };
    }

    static <T> HeaderKey<T> of(String key, T defaultValue, Type type) {
        return new HeaderKey<T>() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public T getDefaultValue() {
                return defaultValue;
            }

            @Override
            public Type getValueType() {
                return  type;
            }
        };
    }

    @SuppressWarnings("all")
    static <T> HeaderKey<T> of(String key, T defaultValue) {
        return of(key, defaultValue, defaultValue == null ? (Class<T>) Object.class : (Class<T>) defaultValue.getClass());
    }
}
