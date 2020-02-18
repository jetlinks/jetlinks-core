package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.UnitSupported;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.unit.ValueUnit;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter
public abstract class NumberType<N extends Number> extends AbstractType<NumberType<N>> implements UnitSupported, DataType, Converter<N> {

    //最大值
    private Number max;

    //最小值
    private Number min;

    //单位
    private ValueUnit unit;

    public NumberType<N> unit(ValueUnit unit) {
        this.unit = unit;
        return this;
    }

    public NumberType<N> max(Number max) {
        this.max = max;
        return this;
    }

    public NumberType<N> min(Number min) {
        this.min = min;
        return this;
    }


    public Object format(Object value) {
        if (value == null) {
            return null;
        }
        ValueUnit unit = getUnit();
        if (unit == null) {
            return String.valueOf(value);
        }
        return unit.format(value);
    }

    @Override
    public ValidateResult validate(Object value) {
        try {
            N numberValue = convert(value);
            if (numberValue == null) {
                return ValidateResult.fail("数字格式错误:" + value);
            }
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

    public N convertNumber(Object value, Function<Number, N> mapper) {
        return Optional.ofNullable(convertNumber(value))
                .map(mapper)
                .orElse(null);
    }

    public Number convertNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value);
        }
        if (value instanceof String) {
            try {
                return new BigDecimal(((String) value));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        return null;
    }

    public abstract N convert(Object value);

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
