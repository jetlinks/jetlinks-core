package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.utils.time.DateFormatter;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Getter
@Setter
public class DateTimeType extends AbstractType<DateTimeType> implements DataType, Converter<Date> {
    public static final String ID = "date";

    public static final String TIMESTAMP_FORMAT = "timestamp";

    private String format = TIMESTAMP_FORMAT;

    private ZoneId zoneId = ZoneId.systemDefault();

    private DateTimeFormatter formatter;

    public DateTimeType timeZone(ZoneId zoneId) {
        this.zoneId = zoneId;

        return this;
    }

    public DateTimeType format(String format) {
        this.format = format;
        this.getFormatter();
        return this;
    }


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "时间";
    }

    protected DateTimeFormatter getFormatter() {
        if (formatter == null) {
            formatter = DateTimeFormatter.ofPattern(format);
        }
        return formatter;
    }

    @Override
    public ValidateResult validate(Object value) {
        if (convert(value) == null) {
            return ValidateResult.fail("不是合法的时间格式");
        }
        return ValidateResult.success();
    }

    @Override
    public String format(Object value) {
        if (value instanceof Number && TIMESTAMP_FORMAT.equals(format)) {
            return value.toString();
        }

        Date dateValue = convert(value);
        if (dateValue == null) {
            return "";
        }
        return LocalDateTime
                .ofInstant(dateValue.toInstant(), zoneId)
                .format(getFormatter());
    }

    public Date convert(Object value) {

        if (value instanceof Instant) {
            return Date.from(((Instant) value));
        }
        if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value).atZone(zoneId).toInstant());

        }

        if (value instanceof Date) {
            return ((Date) value);
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        if (value instanceof String) {
            Date data = DateFormatter.fromString(((String) value));
            if (data != null) {
                return data;
            }
            return Date.from(LocalDateTime.parse(((String) value), getFormatter())
                    .atZone(zoneId)
                    .toInstant());
        }

        return null;
    }


}
