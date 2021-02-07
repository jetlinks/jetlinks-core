package org.jetlinks.core;

import java.util.Date;

public interface Value {
    default String asString() {
        return String.valueOf(get());
    }

    default int asInt() {
        return as(Integer.class);
    }

    default long asLong() {
        return as(Long.class);
    }

    default boolean asBoolean() {
        return Boolean.TRUE.equals(get())
                || "true".equals(get());
    }

    default Number asNumber() {
        return as(Number.class);
    }

    default Date asDate() {
        return as(Date.class);
    }

    Object get();

    <T> T as(Class<T> type);

    static Value simple(Object value) {
        return SimpleValue.of(value);
    }
}
