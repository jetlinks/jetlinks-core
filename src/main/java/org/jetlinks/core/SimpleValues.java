package org.jetlinks.core;

import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.CompositeMap;
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
        return new HashMap<>(values);
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

        @SuppressWarnings("all")
        Map<String, Object> merged = new CompositeMap(this.values, sourceValues);

        return Values.of(merged);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public Collection<String> getNonExistentKeys(Collection<String> keys) {
        return Collections2.filter(keys, key -> !values.containsKey(key));
    }

    @Override
    public String getString(String key, Supplier<String> defaultValue) {
        if (MapUtils.isEmpty(values)) {
            return defaultValue.get();
        }
        Object val = values.get(key);
        if (val == null) {
            return defaultValue.get();
        }
        return String.valueOf(val);
    }

    @Override
    public Number getNumber(String key, Supplier<Number> defaultValue) {
        if (MapUtils.isEmpty(values)) {
            return defaultValue.get();
        }
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
}
