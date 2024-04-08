package org.jetlinks.core.utils;

import java.util.concurrent.TimeUnit;

public class TimestampUtils {

    public static long toMillis(long timestamp) {
        return NumberUtils.fixLength(timestamp, (int) Math.log10(System.currentTimeMillis()) + 1);
    }

    public static long toNanos(long timestamp) {

        return TimeUnit.MILLISECONDS.toNanos(toMillis(timestamp))  ;
    }

}
