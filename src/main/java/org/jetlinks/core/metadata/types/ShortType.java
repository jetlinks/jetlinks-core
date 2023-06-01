package org.jetlinks.core.metadata.types;

import lombok.Generated;

@Generated
public class ShortType extends NumberType<Short> {
    public static final String ID = "short";

    public static final ShortType GLOBAL = new ShortType();

    private static final int SCALE = Integer.getInteger("jetlinks.type.short.scale", 0);

    @Override
    @Generated
    public String getId() {
        return ID;
    }

    @Override
    @Generated
    public String getName() {
        return "短整型";
    }

    @Override
    protected Short castNumber(Number number) {
        return number.shortValue();
    }

    @Override
    protected int defaultScale() {
        return SCALE;
    }
}
