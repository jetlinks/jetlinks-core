package org.jetlinks.core.metadata.types;

public interface DoubleType extends NumberType {
    @Override
    default String getId() {
        return "double";
    }

    @Override
    default String getName() {
        return "双精度浮点数";
    }

    @Override
    default String getDescription() {
        return "双精度浮点数";
    }
}
