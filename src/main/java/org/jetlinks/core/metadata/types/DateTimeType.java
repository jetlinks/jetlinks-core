package org.jetlinks.core.metadata.types;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Getter
@Setter
public class DateTimeType implements DataType {
    public static final String ID = "date";

    public static final String TIMESTAMP_FORMAT = "timestamp";

    private String format = TIMESTAMP_FORMAT;

    private String tzOffset = "+8";

    private DateTimeFormatter formatter;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "时间";
    }

    @Override
    public String getDescription() {
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
                .ofInstant(dateValue.toInstant(), ZoneOffset.of(tzOffset))
                .format(getFormatter());
    }

    protected Date convert(Object value) {

        if (value instanceof Instant) {
            return Date.from(((Instant) value));
        }
        if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value).toInstant(ZoneOffset.of(tzOffset)));

        }

        if (value instanceof Date) {
            return ((Date) value);
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        if (value instanceof String) {
            return Date.from(LocalDateTime.parse(((String) value), getFormatter())
                    .toInstant(ZoneOffset.of(tzOffset)));
        }

        return null;
    }


}
