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

            Assert.assertTrue(t > t - 10 && t < t + 10);
        }

    }


    @Test
    @SneakyThrows
    public void testReset() {
        ParallelIntervalHelper helper = ParallelIntervalHelper.create(Duration.ofSeconds(1));

        Assert.assertEquals(helper.next("test"), 0);
        Assert.assertEquals(helper.next("test"), 1000);
        Thread.sleep(2000);

        Assert.assertEquals(helper.next("test"), 0);
        Assert.assertEquals(helper.next("test"), 1000);
        Assert.assertEquals(helper.next("test"), 2000);
        Assert.assertEquals(helper.next("test"), 3000);
        Assert.assertEquals(helper.next("test"), 4000);

        Thread.sleep(2100);
        long next = helper.next("test");
        System.out.println(next);
        Assert.assertTrue(next > 2800 && next < 2900);

    }


}