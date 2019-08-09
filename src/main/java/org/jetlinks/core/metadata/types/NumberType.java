package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.DataType;

import java.util.Optional;

public interface NumberType extends DataType {
    @Override
    default String getId() {
        return "number";
    }

    Number getMax();

    Number getMin();

    default long getMax(long defaultVal) {
        return Optional
                .ofNullable(getMax())
                .map(Number::longValue)
                .orElse(defaultVal);
    }

    default long getMin(long defaultVal) {
        return Optional
                .ofNullable(getMin())
                .map(Number::longValue)
                .orElse(defaultVal);
    }

    default double getMax(double defaultVal) {
        return Optional
                .ofNullable(getMax())
                .map(Number::doubleValue)
                .orElse(defaultVal);
    }

    default double getMin(double defaultVal) {
        return Optional
                .ofNullable(getMin())
                .map(Number::doubleValue)
                .orElse(defaultVal);
    }
}
