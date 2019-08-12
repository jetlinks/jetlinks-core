package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.Formattable;
import org.jetlinks.core.metadata.ValidateResult;

@Getter
@Setter
public class BooleanType implements DataType, Formattable, Converter<Boolean> {
    public static final String ID = "boolean";

    private String trueText = "是";

    private String falseText = "否";

    private String trueValue = "true";

    private String falseValue = "false";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "布尔值";
    }

    @Override
    public String getDescription() {
        return "布尔类型";
    }

    public Boolean convert(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value);
        }

        String stringVal = String.valueOf(value).trim();
        if (stringVal.equals(trueValue) || stringVal.equals(trueText)) {
            return true;
        }

        if (stringVal.equals(falseValue) || stringVal.equals(falseText)) {
            return false;
        }

        return null;
    }

    @Override
    public ValidateResult validate(Object value) {

        Boolean trueOrFalse = convert(value);

        return trueOrFalse == null
                ? ValidateResult.fail("不支持的值:" + value)
                : ValidateResult.success();
    }

    @Override
    public String format(Object value) {
        Boolean trueOrFalse = convert(value);

        if (Boolean.TRUE.equals(trueOrFalse)) {
            return trueText;
        }
        if (Boolean.FALSE.equals(trueOrFalse)) {
            return falseText;
        }
        return "未知:" + value;
    }


}
