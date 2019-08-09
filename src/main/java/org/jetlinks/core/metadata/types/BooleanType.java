package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.Copyable;
import org.jetlinks.core.metadata.DataType;

public interface BooleanType extends Copyable<BooleanType>, DataType {

    @Override
    default String getId() {
        return "boolean";
    }

    @Override
    default String getName() {
        return "布尔";
    }

    @Override
    default String getDescription() {
        return "布尔类型";
    }

    String getTrueText();

    String getFalseText();

    String getTrueValue();

    String getFalseValue();

}
