package org.jetlinks.core.utils;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class ParallelIntervalHelperTest {

    @Test
    @SneakyThrows
    public void test() {
        ParallelIntervalHelper helper = ParallelIntervalHelper.create(Duration.ofSeconds(1));

        for (int i = 0; i < 1000; i++) {
            long t = helper.next("test");
            assertRange(t, t - 10, t + 10);
        }

    }


    @Test
    @SneakyThrows
    public void testReset() {
        ParallelIntervalHelper helper = ParallelIntervalHelper.create(Duration.ofSeconds(1));

        Assert.assertEquals(helper.next("test"), 0);
        assertRange(helper.next("test"), 990, 1000);
        Thread.sleep(2000);

        Assert.assertEquals(helper.next("test"), 0);
        assertRange(helper.next("test"), 990, 1000);
        assertRange(helper.next("test"), 1990, 2000);
        assertRange(helper.next("test"), 2990, 3000);
        assertRange(helper.next("test"), 3990, 4000);

        Thread.sleep(2100);
        long next = helper.next("test");
        System.out.println(next);
        assertRange(next, 2890, 2900);

    }

    void assertRange(long value, long gte, long lte) {
        Assert.assertTrue(lte + " <= expect(" + value + ") >= " + gte, value >= gte && value <= lte);
    }

}