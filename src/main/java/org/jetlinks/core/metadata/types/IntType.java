package org.jetlinks.core.metadata.types;

import java.util.Map;

public class IntType extends NumberType<Integer> {
    public static final String ID = "int";

    private Map<String, Object> expands;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "整型";
    }


    @Override
    public Integer convert(Object value) {
        return super.convertNumber(value, Number::intValue);
    }
}
