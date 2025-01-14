package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;

@Getter
@Setter
@SuppressWarnings("all")
public class DoubleType extends NumberType<Double> {
    public static final String ID = "double";
    private static final int SCALE = Integer.getInteger("jetlinks.type.double.scale", 2);

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
        return LocaleUtils.resolveMessage("data.type." + getId(), LocaleUtils.current(), "双精度浮点数");
    }

    @Override
    public int defaultScale() {
        return SCALE;
    }
}
