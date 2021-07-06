package org.jetlinks.core.utils;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

public class StringBuilderUtilsTest {


    @Test
    public void test() {

        assertEquals(StringBuilderUtils
                             .buildString(1, (num, build) -> build.append(num)), "1");

    }


    @Test
    public void testMultiThread() {
        int count = 10000;

        Flux.range(1, count)
            .delayElements(Duration.ofNanos(10))
            .map(i -> StringBuilderUtils.buildString(i, (num, build) -> {
                build.append(num);
            }))
            .map(Long::parseLong)
            .reduce(Math::addExact)
            .as(StepVerifier::create)
            .expectNext((count + 1L) * (count / 2))
            .verifyComplete();


    }
}