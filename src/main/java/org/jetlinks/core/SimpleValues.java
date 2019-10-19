package org.jetlinks.core;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "of")
class SimpleValues implements Values {

    @NonNull
    private Map<String, Object> values;

    @Override
    public Map<String, Object> getAllValues() {
        return new HashMap<>(values);
    }

    @Override
    public Optional<Value> getValue(String key) {
        return Optional.ofNullable(key)
                .map(values::get)
                .map(Value::simple);
    }

    @Override
    public Values merge(Values source) {
        Map<String, Object> merged = new HashMap<>();
        merged.putAll(this.values);
        merged.putAll(source.getAllValues());
        return Values.of(merged);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public Set<String> getNonExistentKeys(Collection<String> keys) {
        return keys
                .stream()
                .filter(has -> !values.containsKey(has))
                .collect(Collectors.toSet());
    }
}
