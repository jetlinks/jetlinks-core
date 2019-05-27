package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.ValidateResult;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class IntType implements DataType, Jsonable {

    private Integer max;

    private Integer min;

    private JetlinksStandardValueUnit unit;

    @Override
    public ValidateResult validate(Object value) {

        if (value instanceof Number) {
            int intVal = ((Number) value).intValue();
            if (max != null && intVal > max) {
                return ValidateResult.fail("超过最大值限制:" + max);
            }
            if (min != null && intVal < min) {
                return ValidateResult.fail("低于最小值限制:" + max);
            }
            return ValidateResult.success();
        }

        return ValidateResult.fail("值[" + value + "]不是数字");
    }

    @Override
    public String getId() {
        return "int";
    }

    @Override
    public String getName() {
        return "整型";
    }

    @Override
    public String getDescription() {
        return "整型数字";
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("max", max);
        json.put("min", min);

        if (unit != null) {
            json.put("unit", unit.toJson());
        }

        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        ofNullable(json.getInteger("max"))
                .ifPresent(this::setMax);
        ofNullable(json.getInteger("min"))
                .ifPresent(this::setMin);
        ofNullable(json.get("unit"))
                .map(JetlinksStandardValueUnit::of)
                .ifPresent(this::setUnit);
    }

    @Override
    public String toString() {
        return  toJson().toJSONString();
    }
}
