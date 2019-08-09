package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.Copyable;

public interface LongType extends NumberType, Copyable<LongType> {
    @Override
    default String getId() {
        return "long";
    }

    @Override
    default String getName() {
        return "长整型";
    }

    @Override
    default String getDescription() {
        return "64位整型数字";
    }
}
