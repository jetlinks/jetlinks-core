package org.jetlinks.core.utils;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.*;

public class ExceptionUtilsTest {

    @Test
    public void testBenchmark() {
        StackTraceElement traceElement = new StackTraceElement(
            "org.jetlinks.pro.Test",
            "test",
            "Test.java",
            1
        );

        long nano = System.nanoTime();
        for (int i = 0; i < 100_0000; i++) {
            ExceptionUtils.isUnimportant(traceElement);
        }

        System.out.println(Duration.ofNanos(System.nanoTime() - nano));
    }

}