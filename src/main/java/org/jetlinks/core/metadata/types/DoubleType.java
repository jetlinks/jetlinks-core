package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("all")
public class DoubleType extends NumberType<Double> {
    public static final String ID = "double";
    private static final int SCALE = Integer.getInteger("jetlinks.type.int.scale", 2);

    public static final DoubleType GLOBAL = new DoubleType();

    @Override
    protected Double castNumber(Number number) {
        return number.doubleValue();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "双精度浮点数";
    }

    @Override
    public int defaultScale() {
        return SCALE;
    }
}
