package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.FormatSupport;
import org.jetlinks.core.metadata.ValidateResult;

@Getter
@Setter
public class BooleanType extends AbstractType<BooleanType> implements DataType, FormatSupport, Converter<Boolean> {
    public static final String ID = "boolean";

    public static final BooleanType GLOBAL = new BooleanType();

    private String trueText = "是";

    private String falseText = "否";

    private String trueValue = "true";

    private String falseValue = "false";

    public BooleanType trueText(String trueText){
        this.trueText=trueText;
        return this;
    }

    public BooleanType falseText(String falseText){
        this.falseText=falseText;
        return this;
    }

    public BooleanType trueValue(String trueValue){
        this.trueValue=trueValue;
        return this;
    }

    public BooleanType falseValue(String falseValue){
        this.falseText=falseValue;
        return this;
    }
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
                : ValidateResult.success(trueOrFalse);
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
