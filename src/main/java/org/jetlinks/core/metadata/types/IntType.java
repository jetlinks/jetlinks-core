package org.jetlinks.core.metadata.types;

public class IntType extends NumberType<Integer> {
    public static final String ID = "int";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "整型";
    }

    @Override
    public String getDescription() {
        return "32位整型数字";
    }

    @Override
    public Integer convert(Object value) {
        return super.convertNumber(value, Number::intValue);
    }
}
