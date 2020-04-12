package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

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
    public Long convert(Object value) {
        return super.convertNumber(value,Number::longValue);
    }
}
