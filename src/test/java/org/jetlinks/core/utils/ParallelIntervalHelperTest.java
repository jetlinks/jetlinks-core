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
            Assert.assertEquals(helper.next("test"), i * 1000);
        }
        Thread.sleep(1200);
        for (int i = 0; i < 1000; i++) {
            Assert.assertEquals(helper.next("test"), i * 1000);
        }
    }

}