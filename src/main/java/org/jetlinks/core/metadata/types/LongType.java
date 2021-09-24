package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

import java.math.RoundingMode;

@Getter
@Setter
public class LongType extends NumberType<Long> {
    public static final String ID = "long";
    public static final LongType GLOBAL = new LongType();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "长整型";
    }

    @Override
    public Number convertScaleNumber(Object value) {
        return super.convertScaleNumber(value, 0, RoundingMode.HALF_UP, Number::longValue);
    }

    @Override
    public Long convert(Object value) {
        return super.convertNumber(value, Number::longValue);
    }
}
