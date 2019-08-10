package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.UnitSupported;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.unit.ValueUnit;

import java.util.Optional;

@Getter
@Setter
public abstract class NumberType implements UnitSupported, DataType {

    //最大值
    private Number max;

    //最小值
    private Number min;

    //单位
    private ValueUnit unit;

    public Object format(Object value) {
        ValueUnit unit = getUnit();
        if (unit == null) {
            return String.valueOf(value);
        }
        return unit.format(value);
    }

    @Override
    public ValidateResult validate(Object value) {
        try {
            Number numberValue = convert(value);

            if (max != null && numberValue.doubleValue() > max.doubleValue()) {
                return ValidateResult.fail("超过最大值:" + max);
            }
            if (min != null && numberValue.doubleValue() < min.doubleValue()) {
                return ValidateResult.fail("超过最大值:" + min);
            }
            return ValidateResult.success(numberValue);
        } catch (NumberFormatException e) {
            return ValidateResult.fail(e.getMessage());
        }
    }

    protected Number convert(Object value) {
        if (value instanceof Number) {
            return ((Number) value);
        }
        throw new NumberFormatException("数字格式错误:" + value);
    }

    public long getMax(long defaultVal) {
        return Optional
                .ofNullable(getMax())
                .map(Number::longValue)
                .orElse(defaultVal);
    }

    public long getMin(long defaultVal) {
        return Optional
                .ofNullable(getMin())
                .map(Number::longValue)
                .orElse(defaultVal);
    }

    public double getMax(double defaultVal) {
        return Optional
                .ofNullable(getMax())
                .map(Number::doubleValue)
                .orElse(defaultVal);
    }

    public double getMin(double defaultVal) {
        return Optional
                .ofNullable(getMin())
                .map(Number::doubleValue)
                .orElse(defaultVal);
    }
}
