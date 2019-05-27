package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@AllArgsConstructor
@SuppressWarnings("all")
public class DefaultValueWrapper implements ValueWrapper {

    private Object value;

    @Override
    public Optional<Object> value() {
        return Optional.ofNullable(value);
    }

    @Override
    public Optional<String> asString() {
        return value().map(String::valueOf);
    }

    @Override
    public Optional<Integer> asInteger() {
        return value().map(Integer.class::cast);
    }

    @Override
    public Optional<Boolean> asBoolean() {
        return value().map(Boolean.class::cast);
    }

    @Override
    public <T> Optional<T> as(Class<T> type) {

        return value()
                .map(v -> {
                    if (type.isInstance(v)) {
                        return (T) v;
                    }
                    if (v instanceof String) {
                        return JSON.parseObject((String) v, type);
                    }
                    throw new UnsupportedOperationException("无法将" + v + "转为" + type);
                });
    }

    @Override
    public <T> Optional<List<T>> asList(Class<T> type) {
        return value()
                .map(v -> {
                    if (v instanceof List) {
                        return (List<T>) v;
                    }
                    if (v instanceof String) {
                        return JSON.parseArray((String) v, type);
                    }
                    throw new UnsupportedOperationException("无法将" + v + "转为" + type);
                });
    }

    @Override
    public boolean isPresent() {
        return value != null;
    }
}
