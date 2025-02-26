package org.jetlinks.core.utils;

import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.TraceSourceException;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.*;

public class ExceptionUtilsTest {
static {
    ExceptionUtils.addUnimportantPackage("org.junit",
                                         "com.intellij",
                                         "sun.reflect");
}
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

    @Test
    public void testMergeStackTrace() {
        Throwable e = TraceSourceException.transform(new RuntimeException(
            new NotFoundException("not_found")

        ), "test", "AAA");

        e.printStackTrace();

        System.out.println();

        System.out.println(ExceptionUtils.getStackTrace(e).length());

        for (StackTraceElement traceElement : ExceptionUtils.getMergedStackTrace(e)) {
            System.out.println(traceElement);
        }
    }

}