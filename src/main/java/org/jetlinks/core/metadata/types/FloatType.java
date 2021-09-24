package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

import java.math.RoundingMode;

@Getter
@Setter
@SuppressWarnings("all")
public class FloatType extends NumberType<Float> {
    public static final String ID = "float";

    private Integer scale;
    public static final FloatType GLOBAL = new FloatType();

    @Override
    public Float convertScaleNumber(Object value) {
        return convertScaleNumber(value, this.scale, RoundingMode.HALF_UP, Number::floatValue);
    }

    public FloatType scale(Integer scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public Float convert(Object value) {
        return super.convertNumber(value, Number::floatValue);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "单精度浮点数";
    }


}
