package org.jetlinks.core;

import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetlinks.core.utils.CompositeMap;
import org.jetlinks.core.utils.ConverterUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;

@AllArgsConstructor(staticName = "of")
class SimpleValues implements Values {

    @NonNull
    private final Map<String, Object> values;

    @Override
    public Map<String, Object> getAllValues() {
        return values instanceof CompositeMap ? values : Collections.unmodifiableMap(values);
    }

    @Override
    public Optional<Value> getValue(String key) {
        if (key == null) {
            return Optional.empty();
        }
        Object value = values.get(key);
        if (null == value) {
            return Optional.empty();
        }
        return Optional.of(Value.simple(value));
    }

    @Override
    public Values merge(Values source) {
        Map<String, Object> sourceValues = source instanceof SimpleValues ? ((SimpleValues) source).values : source.getAllValues();

        Map<String, Object> values = new CompositeMap<>(sourceValues, this.values);

        return Values.of(values);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean isNoEmpty() {
        return !values.isEmpty();
    }

    @Override
    public Collection<String> getNonExistentKeys(Collection<String> keys) {
        return Collections2.filter(keys, key -> !values.containsKey(key));
    }

    @Override
    public String getString(String key, Supplier<String> defaultValue) {
        Object val = values.get(key);
        if (val == null) {
            return defaultValue.get();
        }
        return String.valueOf(val);
    }

    @Override
    public Number getNumber(String key, Supplier<Number> defaultValue) {
        Object val = values.get(key);
        if (val == null) {
            return defaultValue.get();
        }
        if (val instanceof Number) {
            return ((Number) val);
        }
        if (val instanceof Date) {
            return ((Date) val).getTime();
        }
        return ConverterUtils.convert(val, BigDecimal.class);
    }

    @Override
    public String toString() {
        return String.valueOf(values);
    }
}
