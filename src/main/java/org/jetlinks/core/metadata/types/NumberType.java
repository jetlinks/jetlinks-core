package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.UnitSupported;
import org.jetlinks.core.metadata.ValidateResult;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.utils.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter
public abstract class NumberType<N extends Number> extends AbstractType<NumberType<N>> implements UnitSupported, DataType, Converter<N> {

    static boolean ORIGINAL = Boolean.parseBoolean(System.getProperty(
        "jetlinks.type.number.convert.original", "true"));

    static boolean FORMAT_STRIP_TRAILING_ZEROS = Boolean.parseBoolean(System.getProperty(
        "jetlinks.type.number.format.stripTrailingZeros", "false"));

    //最大值
    private Number max;

    //最小值
    private Number min;

    //单位
    private ValueUnit unit;

    private RoundingMode round = defaultRound();

    private Integer scale;

    public NumberType<N> scale(Integer scale) {
        this.scale = scale;
        return this;
    }

    public NumberType<N> round(RoundingMode round) {
        this.round = round;
        return this;
    }

    public Integer getScale(Integer defaultValue) {
        return this.scale == null ? defaultValue : this.scale;
    }

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
        Number val = convertScaleNumber(value,
                                        this.getScale(defaultScale()),
                                        getRound(),
                                        Function.identity());
        if (val == null) {
            return String.valueOf(value);
        }
        String str;
        if (val instanceof BigDecimal) {
            if (FORMAT_STRIP_TRAILING_ZEROS) {
                val = ((BigDecimal) val).stripTrailingZeros();
            }
            str = ((BigDecimal) val).toPlainString();
        } else {
            str = String.valueOf(val);
        }
        ValueUnit unit = getUnit();
        if (unit == null) {
            return String.valueOf(str);
        }
        return unit.format(str);
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

    public final N convertNumber(Object value) {
        //保持原始值
        if (ORIGINAL) {
            return convertOriginalNumber(value);
        }
        return convertScaleNumber(value);
    }

    public final N convertOriginalNumber(Object value) {
        return convertScaleNumber(value, null, null, this::castNumber);
    }

    public final N convertScaleNumber(Object value,
                                      Integer scale,
                                      RoundingMode mode) {
        return convertScaleNumber(value, scale, mode, this::castNumber);
    }

    public final N convertScaleNumber(Object value) {
        return convertScaleNumber(value, this.getScale(defaultScale()), getRound());
    }

    @Override
    public final N convert(Object value) {
        if (value instanceof Number) {
            Number number = ((Number) value);
            //如果传入的是整数,或者未设置精度,则直接返回
            if (NumberUtils.isIntNumber(number)) {
                return castNumber(number);
            }
        }
        return this.convertNumber(value);
    }

    public final long getMax(long defaultVal) {
        return Optional
            .ofNullable(getMax())
            .map(Number::longValue)
            .orElse(defaultVal);
    }

    public final long getMin(long defaultVal) {
        return Optional
            .ofNullable(getMin())
            .map(Number::longValue)
            .orElse(defaultVal);
    }

    public final double getMax(double defaultVal) {
        return Optional
            .ofNullable(getMax())
            .map(Number::doubleValue)
            .orElse(defaultVal);
    }

    public final double getMin(double defaultVal) {
        return Optional
            .ofNullable(getMin())
            .map(Number::doubleValue)
            .orElse(defaultVal);
    }

    protected abstract N castNumber(Number number);

    protected abstract int defaultScale();

    protected RoundingMode defaultRound() {
        return RoundingMode.valueOf(System.getProperty("jetlinks.type." + getType() + ".round", "HALF_UP"));
    }

    public static <T> T convertScaleNumber(Object value,
                                           Integer scale,
                                           RoundingMode mode,
                                           Function<Number, T> mapper) {
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


}
