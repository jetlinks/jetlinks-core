package org.jetlinks.core;

public interface Value {
    default String asString() {
        return as(String.class);
    }

    default int asInt() {
        return as(Integer.TYPE);
    }

    default long asLong() {
        return as(Long.TYPE);
    }

    default boolean asBoolean() {
        return as(Boolean.TYPE);
    }

    Object get();

    <T> T as(Class<T> type);

    static Value simple(Object value) {
        return SimpleValue.of(value);
    }
}
