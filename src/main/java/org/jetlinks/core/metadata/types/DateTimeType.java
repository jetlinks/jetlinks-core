package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.ValidateResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.util.Optional.ofNullable;

@Getter
@Setter
@Slf4j
public class DateTimeType extends AbstractType<DateTimeType> implements DataType, Converter<Date> {
    public static final String ID = "date";

    public static final String TIMESTAMP_FORMAT = "timestamp";

    public static final DateTimeType GLOBAL = new DateTimeType();

    private String format = TIMESTAMP_FORMAT;

    private ZoneId zoneId = ZoneId.systemDefault();

    private DateTimeFormatter formatter;


    static {
        DateFormatter.supportFormatter.add(new ISODateTimeFormatter());
    }

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
        return LocaleUtils.resolveMessage("message.metadata.type.date", LocaleUtils.current(), "时间");
    }

    protected DateTimeFormatter getFormatter() {
        if (formatter == null && !TIMESTAMP_FORMAT.equals(format)) {
            formatter = DateTimeFormatter.ofPattern(format);
        }
        return formatter;
    }

    @Override
    public ValidateResult validate(Object value) {
        if ((value = convert(value)) == null) {
            return ValidateResult.fail("不是合法的时间格式");
        }
        return ValidateResult.success(value);
    }

    @Override
    public String format(Object value) {
        try {
            if (TIMESTAMP_FORMAT.equals(format)) {
                return String.valueOf(convert(value).getTime());
            }
            Date dateValue = convert(value);
            if (dateValue == null) {
                return "";
            }
            return LocalDateTime
                    .ofInstant(dateValue.toInstant(), zoneId)
                    .format(getFormatter());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return "";
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
            if(StringUtils.isNumber(value)){
                return new Date(Long.parseLong((String) value));
            }
            Date data = DateFormatter.fromString(((String) value));
            if (data != null) {
                return data;
            }
            DateTimeFormatter formatter = getFormatter();
            if (null == formatter) {
                throw new IllegalArgumentException("unsupported date format:" + value);
            }
            return Date.from(LocalDateTime.parse(((String) value), formatter)
                    .atZone(zoneId)
                    .toInstant());
        }
        throw new IllegalArgumentException("can not format datetime :" + value);
    }


    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        json.put("format", this.getFormat());
        json.put("tz", this.getZoneId().toString());
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        super.fromJson(json);
        ofNullable(json.getString("format"))
                .ifPresent(this::setFormat);
        ofNullable(json.getString("tz"))
                .map(ZoneId::of)
                .ifPresent(this::setZoneId);

    }
}
