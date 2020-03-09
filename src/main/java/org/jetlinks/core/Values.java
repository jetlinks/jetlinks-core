package org.jetlinks.core;

import org.jetlinks.core.config.ConfigKey;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    static Values of(Map<String, Object> values) {
        return SimpleValues.of(values);
    }
}
