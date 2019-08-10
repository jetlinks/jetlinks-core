package org.jetlinks.core.metadata.types;

import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

public class StringType implements DataType {
    public static final String ID = "string";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "字符串";
    }

    @Override
    public String getDescription() {
        return "普通文本类型";
    }


    @Override
    public ValidateResult validate(Object value) {

        return ValidateResult.success(String.valueOf(value));
    }

    @Override
    public String format(Object value) {
        return String.valueOf(value);
    }
}
