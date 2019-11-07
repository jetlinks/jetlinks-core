package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.FormatSupport;
import org.jetlinks.core.metadata.ValidateResult;

import java.util.Map;

@Getter
@Setter
public class BooleanType implements DataType, FormatSupport, Converter<Boolean> {
    public static final String ID = "boolean";

    private String trueText = "是";

    private String falseText = "否";

    private String trueValue = "true";

    private String falseValue = "false";

    private String description;

    private Map<String, Object> expands;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "布尔值";
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
