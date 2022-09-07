package org.jetlinks.core;

import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.CompositeMap;
import org.hswebframework.web.bean.FastBeanCopier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
        return Optional
                .ofNullable(key)
                .map(values::get)
                .map(Value::simple);
    }

    @Override
    public Values merge(Values source) {
        Map<String, Object> sourceValues = source instanceof SimpleValues ? ((SimpleValues) source).values : source.getAllValues();

        Map<String, Object> merged = new CompositeMap(this.values,sourceValues);

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
        return FastBeanCopier.DEFAULT_CONVERT.convert(
                val, Number.class, FastBeanCopier.EMPTY_CLASS_ARRAY
        );
    }
}
