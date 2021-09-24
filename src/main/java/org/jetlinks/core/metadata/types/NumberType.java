package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.UnitSupported;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.unit.ValueUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            return value;
        }
        return unit.format(convertScaleNumber(value));
    }

    @Override
    public ValidateResult validate(Object value) {
        try {
            Number numberValue = convertScaleNumber(value);
            if (numberValue == null) {
                return ValidateResult.fail("数字格式错误:" + value);
            }
            if (max != null && numberValue.doubleValue() > max.doubleValue()) {
                return ValidateResult.fail("超过最大值:" + max);
            }
            if (min != null && numberValue.doubleValue() < min.doubleValue()) {
                return ValidateResult.fail("小于最小值:" + min);
            }
            return ValidateResult.success(numberValue);
        } catch (NumberFormatException e) {
            return ValidateResult.fail(e.getMessage());
        }
    }

    public final <T> T convertNumber(Object value, Function<Number, T> mapper) {
        if (value instanceof Number) {
            return mapper.apply((Number) value);
        }
        return Optional
                .ofNullable(convertScaleNumber(value, null, null, mapper))
                .orElse(null);
    }

    public Number convertScaleNumber(Object value) {
        return convertNumber(value, Function.identity());
    }

    public final <T> T convertScaleNumber(Object value, Integer scale, RoundingMode mode, Function<Number, T> mapper) {
        BigDecimal decimal;
        if (value instanceof Number && scale == null) {
            return mapper.apply(((Number) value));
        }
        if (value instanceof String) {
            try {
                value = new BigDecimal(((String) value));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value instanceof Date) {
            value = new BigDecimal(((Date) value).getTime());
        }
        if (!(value instanceof BigDecimal)) {
            try {
                decimal = new BigDecimal(String.valueOf(value));
            } catch (Throwable err) {
                return null;
            }
        } else {
            decimal = ((BigDecimal) value);
        }
        if (scale == null) {
            return mapper.apply(decimal);
        }
        return mapper.apply(decimal.setScale(scale, mode));
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
