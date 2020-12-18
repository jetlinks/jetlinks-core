package org.jetlinks.core.utils;

public class TimestampUtils {

    public static long toMillis(long timestamp) {
        return NumberUtils.fixLength(timestamp, 13);
    }

    public static long toNanos(long timestamp) {
        return NumberUtils.fixLength(timestamp, 15);
    }


}
