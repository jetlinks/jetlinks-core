package org.jetlinks.core.metadata;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface ValueWrapper {
    Optional<Object> value();

    Optional<String> asString();

    Optional<Integer> asInteger();

    Optional<Boolean> asBoolean();

    <T> Optional<T> as(Class<T> type);

    <T> Optional<List<T>> asList(Class<T> type);

    default ValueWrapper notPresent(Supplier<ValueWrapper> supplier) {
        if (!isPresent()) {
            return supplier.get();
        }
        return this;
    }

    boolean isPresent();

}
