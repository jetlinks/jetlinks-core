package org.jetlinks.core.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimestampUtilsTest {


    @Test
    public void testToMillis() {
        long ts = System.currentTimeMillis();

        assertEquals(TimestampUtils.toMillis(ts), ts);

        ts = System.currentTimeMillis() / 1000;
        assertEquals(TimestampUtils.toMillis(ts), ts * 1000);

        ts = System.currentTimeMillis() * 1000;
        assertEquals(TimestampUtils.toMillis(ts), ts / 1000);

    }

    @Test
    public void testToNanos() {
        long ts = System.currentTimeMillis();

        System.out.println(TimestampUtils.toNanos(ts));
        assertEquals(ts, TimestampUtils.toMillis(TimestampUtils.toNanos(ts)));

        assertEquals(ts / 1000 * 1000, TimestampUtils.toMillis(TimestampUtils.toNanos(ts / 1000)));

        assertEquals(ts, TimestampUtils.toMillis(TimestampUtils.toNanos(ts * 1000)));


    }
}