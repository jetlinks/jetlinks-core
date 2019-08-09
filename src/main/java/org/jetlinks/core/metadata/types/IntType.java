package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.Copyable;

public interface IntType extends NumberType, Copyable<IntType> {
    @Override
    default String getId() {
        return "int";
    }

    @Override
    default String getName() {
        return "整型";
    }

    @Override
    default String getDescription() {
        return "32位整型数字";
    }
}
