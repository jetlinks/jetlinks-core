package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

import java.math.RoundingMode;

@Getter
@Setter
@SuppressWarnings("all")
public class DoubleType extends NumberType<Double> {
    public static final String ID = "double";

    private Integer scale;

    public static final DoubleType GLOBAL = new DoubleType();

    public DoubleType scale(Integer scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public Double convertScaleNumber(Object value) {
        return convertScaleNumber(value, this.scale, RoundingMode.HALF_UP,Number::doubleValue);
    }

    @Override
    public Double convert(Object value) {
        return super.convertNumber(value, Number::doubleValue);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "双精度浮点数";
    }

}
