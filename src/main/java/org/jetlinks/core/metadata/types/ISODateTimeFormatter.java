package org.jetlinks.core.metadata.types;

import org.hswebframework.utils.time.DateFormatter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Pattern;

public class ISODateTimeFormatter implements DateFormatter {
    private final Pattern pattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}\\+.*");

    @Override
    public boolean support(String str) {
        return pattern.matcher(str).matches();
    }

    @Override
    public Date format(String str) {
        return Date.from(LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    @Override
    public String toString(Date date) {
        return DateTimeFormatter.ISO_DATE_TIME.format(date.toInstant());
    }

    @Override
    public String getPattern() {
        return "yyyy-MM-dd'T'HH:mm:ss.SSSz";
    }
}
