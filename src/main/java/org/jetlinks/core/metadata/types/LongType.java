package org.jetlinks.core.metadata.types;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;

@Getter
@Setter
@Generated
public class LongType extends NumberType<Long> {
    public static final String ID = "long";
    public static final LongType GLOBAL = new LongType();
    public static final int SCALE = Integer.getInteger("jetlinks.type.long.scale", 0);

    @Override
    @Generated
    public String getId() {
        return ID;
    }

    @Override
    @Generated
    public String getName() {
        return LocaleUtils.resolveMessage("data.type." + getId(), LocaleUtils.current(), "长整型");
    }

    @Override
    protected Long castNumber(Number number) {
        return number.longValue();
    }

    @Override
    public int defaultScale() {
        return SCALE;
    }
}
