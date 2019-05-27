package org.jetlinks.core.support.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.ValidateResult;

import java.math.BigDecimal;

import static java.math.BigDecimal.ROUND_HALF_UP;
import static java.util.Optional.ofNullable;

@Getter
@Setter
//@SuppressWarnings("all")
public class DoubleType implements DataType, Jsonable {

    private Double max;

    private Double min;

    //精度
    private Integer scale;

    private JetlinksStandardValueUnit unit;

    @Override
    public ValidateResult validate(Object value) {

        if (value instanceof Number) {
            double intVal = ((Number) value).doubleValue();
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
    public String format(Object value) {
        if (value instanceof Number) {
            int scale = this.scale == null ? 2 : this.scale;
            double doubleValue = ((Number) value).doubleValue();
            String scaled = new BigDecimal(doubleValue)
                    .setScale(scale, ROUND_HALF_UP)
                    .toString();
            if (unit != null) {
                return unit.format(scaled);
            }
            return scaled;
        }
        return String.valueOf(value);
    }

    @Override
    public String getId() {
        return "double";
    }

    @Override
    public String getName() {
        return "双精度浮点型";
    }

    @Override
    public String getDescription() {
        return "双精度浮点型数字";
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("max", max);
        json.put("min", min);

        json.put("scale", scale);
        if (unit != null) {
            json.put("unit", unit.toJson());
        }
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        ofNullable(jsonObject.getDouble("max"))
                .ifPresent(this::setMax);
        ofNullable(jsonObject.getDouble("min"))
                .ifPresent(this::setMin);
        ofNullable(jsonObject.getInteger("scale"))
                .ifPresent(this::setScale);
        ofNullable(jsonObject.get("unit"))
                .map(JetlinksStandardValueUnit::of)
                .ifPresent(this::setUnit);
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }
}
