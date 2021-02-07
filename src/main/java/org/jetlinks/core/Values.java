package org.jetlinks.core;

import org.jetlinks.core.config.ConfigKey;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public interface Values {

    Map<String, Object> getAllValues();

    Optional<Value> getValue(String key);

    Values merge(Values source);

    int size();

    Set<String> getNonExistentKeys(Collection<String> keys);

    default boolean isEmpty() {
        return size() == 0;
    }

    default boolean isNoEmpty() {
        return size() > 0;
    }

    default <T> Optional<T> getValue(ConfigKey<T> key) {
        return getValue(key.getKey())
                .map(val -> (val.as(key.getType())));
    }

    default String getString(String key, Supplier<String> defaultValue) {
        return getValue(key).map(Value::asString).orElseGet(defaultValue);
    }

    default String getString(String key, String defaultValue) {
        return getString(key, () -> defaultValue);
    }

    default Number getNumber(String key, Supplier<Number> defaultValue) {
        return getValue(key).map(Value::asNumber).orElseGet(defaultValue);
    }

    default Number getNumber(String key, Number defaultValue) {
        return getNumber(key, () -> defaultValue);
    }

    static Values of(Map<String, ?> values) {
        return SimpleValues.of((Map) values);
    }
}
