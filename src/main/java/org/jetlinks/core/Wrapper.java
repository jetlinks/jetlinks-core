package org.jetlinks.core;

public interface Wrapper {

    default boolean isWrapperFor(Class<?> type) {
        return type.isInstance(this);
    }

    default <T> T unwrap(Class<T> type) {
        return type.cast(this);
    }

}
