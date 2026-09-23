package org.jetlinks.core;

import com.google.common.collect.Collections2;
import org.jetlinks.core.utils.CompositeMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

class Values2 implements Values {

    protected final String firstKey;
    protected final Object firstValue;
    protected final String secondKey;
    protected final Object secondValue;

    Values2(String firstKey, Object firstValue, String secondKey, Object secondValue) {
        this.firstKey = firstKey;
        this.firstValue = firstValue;
        this.secondKey = secondKey;
        this.secondValue = secondValue;
    }

    protected Object getRaw(String key) {
        if (key == null) {
            return null;
        }
        if (key.equals(secondKey)) {
            return secondValue;
        }
        return key.equals(firstKey) ? firstValue : null;
    }

    @Override
    public Optional<Value> getValue(String key) {
        return Optional.ofNullable(getRaw(key)).map(Value::simple);
    }

    @Override
    public String getString(String key, Supplier<String> defaultValue) {
        Object value = getRaw(key);
        return value == null ? defaultValue.get() : String.valueOf(value);
    }

    @Override
    public Map<String, Object> getAllValues() {
        if (firstValue == null) {
            return secondValue == null ? Collections.emptyMap() : Collections.singletonMap(secondKey, secondValue);
        }
        if (secondValue == null) {
            return Collections.singletonMap(firstKey, firstValue);
        }
        return Map.of(firstKey, firstValue, secondKey, secondValue);
    }

    @Override
    public Values merge(Values source) {
        return Values.of(new CompositeMap<>(source.getAllValues(), getAllValues()));
    }

    @Override
    public int size() {
        return (firstValue == null ? 0 : 1) + (secondValue == null ? 0 : 1);
    }

    @Override
    public Collection<String> getNonExistentKeys(Collection<String> keys) {
        return Collections2.filter(keys, key -> getRaw(key) == null);
    }
}
