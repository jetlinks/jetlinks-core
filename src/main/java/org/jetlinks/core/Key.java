package org.jetlinks.core;

import java.util.List;
import java.util.function.BinaryOperator;

public interface Key<T> extends BinaryOperator<T> {

    String getKey();

    static <T> Key<T> create(String key) {
        return new Keys.BaseKey<>(key);
    }

    static Key<Integer> intKey(String key) {
        return new Keys.BaseKey<>(key);
    }

    static Key<Long> longKey(String key) {
        return new Keys.BaseKey<>(key);
    }

    static Key<Boolean> booleanKey(String key) {
        return new Keys.BaseKey<>(key);
    }

    static Key<String> stringKey(String key) {
        return new Keys.BaseKey<>(key);
    }

    static <T> Key<List<T>> listKey(String key) {
        return new Keys.ListKey<>(key);
    }
}
