package org.jetlinks.core;

import java.util.*;

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

    static Values of(Map<String, Object> values) {
        return SimpleValues.of(values);
    }
}
