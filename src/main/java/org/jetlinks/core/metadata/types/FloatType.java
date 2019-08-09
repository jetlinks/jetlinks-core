package org.jetlinks.core.metadata.types;

public interface FloatType extends NumberType {
    @Override
    default String getId() {
        return "float";
    }

    @Override
    default String getName() {
        return "单精度浮点数";
    }

    @Override
    default String getDescription() {
        return "单精度浮点数";
    }
}
