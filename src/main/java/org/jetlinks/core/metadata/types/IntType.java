package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

import java.math.RoundingMode;

@Getter
@Setter
public class IntType extends NumberType<Integer> {
    public static final String ID = "int";

    public static final IntType GLOBAL = new IntType();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "整型";
    }

    @Override
    public Number convertScaleNumber(Object value) {
        return super.convertScaleNumber(value, 0, RoundingMode.HALF_UP,Number::intValue);
    }

    @Override
    public Integer convert(Object value) {
        return super.convertNumber(value, Number::intValue);
    }
}
