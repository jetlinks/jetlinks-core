package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import static java.math.BigDecimal.ROUND_HALF_UP;

@Getter
@Setter
@SuppressWarnings("all")
public class DoubleType extends NumberType<Double> {
    public static final String ID = "double";

    private Integer scale;

    public static final DoubleType GLOBAL = new DoubleType();

    @Override
    public Object format(Object value) {
        Number val = convertNumber(value);
        if (val == null) {
            return super.format(value);
        }
        int scale = this.scale == null ? 2 : this.scale;
        String scaled = new BigDecimal(val.toString())
                .setScale(scale, ROUND_HALF_UP)
                .toString();
        return super.format(scaled);
    }

    public DoubleType scale(Integer scale) {
        this.scale = scale;
        return this;
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
