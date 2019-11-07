package org.jetlinks.core.metadata.types;

import java.util.Map;

public class LongType extends NumberType<Long> {
    public static final String ID = "long";

    private Map<String, Object> expands;

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
