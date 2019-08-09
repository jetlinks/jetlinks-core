package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.unit.ValueUnit;

import static java.util.Optional.ofNullable;


@Getter
@Setter
public class DefaultBooleanType implements org.jetlinks.core.metadata.types.BooleanType, Jsonable {

    private String trueText = "是";

    private String falseText = "否";

    private String trueValue = "true";

    private String falseValue = "false";

    protected Boolean convertValue(Object value) {

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

        Boolean trueOrFalse = convertValue(value);

        return trueOrFalse == null
                ? ValidateResult.fail("不支持的值:" + value)
                : ValidateResult.success();
    }

    @Override
    public ValueUnit getUnit() {
        return new ValueUnit() {
            @Override
            public String format(Object value) {
                Boolean trueOrFalse = convertValue(value);

                if (Boolean.TRUE.equals(trueOrFalse)) {
                    return trueText;
                }
                if (Boolean.FALSE.equals(trueOrFalse)) {
                    return falseText;
                }
                return "未知:" + value;
            }

            @Override
            public String getId() {
                return "boolean";
            }

            @Override
            public String getName() {
                return "布尔值";
            }

            @Override
            public String getDescription() {
                return DefaultBooleanType.this.getDescription();
            }
        };
    }

    @Override
    public void copyFrom(BooleanType booleanType) {
        setFalseText(booleanType.getFalseText());
        setFalseValue(booleanType.getFalseValue());
        setTrueText(booleanType.getTrueText());
        setTrueValue(booleanType.getTrueValue());
    }

    @Override
    public String getId() {
        return "boolean";
    }

    @Override
    public String getName() {
        return "布尔类型";
    }

    @Override
    public String getDescription() {
        return String.format("%s(%s)/%s(%s)", trueText, trueValue, falseText, falseValue);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("trueText", trueText);
        json.put("falseText", falseText);
        json.put("trueValue", trueValue);
        json.put("falseValue", falseValue);
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        ofNullable(jsonObject.getString("trueText"))
                .ifPresent(this::setTrueText);
        ofNullable(jsonObject.getString("falseText"))
                .ifPresent(this::setFalseText);
        ofNullable(jsonObject.getString("trueValue"))
                .ifPresent(this::setTrueValue);
        ofNullable(jsonObject.getString("falseValue"))
                .ifPresent(this::setFalseValue);
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }
}
