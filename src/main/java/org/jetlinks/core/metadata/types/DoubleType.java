package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

import static java.math.BigDecimal.ROUND_HALF_UP;

@Getter
@Setter
@SuppressWarnings("all")
public class DoubleType extends NumberType<Double> {
    public static final String ID = "double";

    private Integer scale;

    private Map<String, Object> expands;

    @Override
    public Object format(Object value) {
        if (value instanceof Number) {
            int scale = this.scale == null ? 2 : this.scale;
            String scaled = new BigDecimal(value.toString())
                    .setScale(scale, ROUND_HALF_UP)
                    .toString();
            return super.format(scaled);
        }
        return super.format(value);
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
