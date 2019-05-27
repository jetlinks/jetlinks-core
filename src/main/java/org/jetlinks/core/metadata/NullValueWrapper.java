package org.jetlinks.core.metadata;

import java.util.List;
import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class NullValueWrapper implements ValueWrapper {

    public static final NullValueWrapper instance = new NullValueWrapper();

    private NullValueWrapper() {
    }

    @Override
    public Optional<Object> value() {
        return Optional.empty();
    }

    @Override
    public Optional<String> asString() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> asInteger() {
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> asBoolean() {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> as(Class<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<List<T>> asList(Class<T> type) {
        return Optional.empty();
    }

    @Override
    public boolean isPresent() {
        return false;
    }
}
