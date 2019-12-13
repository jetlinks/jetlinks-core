package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
    public Integer convert(Object value) {
        return super.convertNumber(value, Number::intValue);
    }
}
