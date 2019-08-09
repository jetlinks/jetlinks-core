package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.types.IntType;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class DefaultIntType implements org.jetlinks.core.metadata.types.IntType, Jsonable {

    private Long max;

    private Long min;

    private JetLinksStandardValueUnit unit;

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
        ofNullable(json.getLong("max"))
                .ifPresent(this::setMax);
        ofNullable(json.getLong("min"))
                .ifPresent(this::setMin);
        ofNullable(json.get("unit"))
                .map(JetLinksStandardValueUnit::of)
                .ifPresent(this::setUnit);
    }

    @Override
    public void copyFrom(IntType intType) {
        Optional.ofNullable(intType.getMax())
                .map(Number::longValue)
                .ifPresent(this::setMax);
        Optional.ofNullable(intType.getMin())
                .map(Number::longValue)
                .ifPresent(this::setMin);
        unit = JetLinksStandardValueUnit.of(intType.getUnit());

    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }
}
