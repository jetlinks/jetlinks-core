package org.jetlinks.core.utils;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.Assert.*;

public class DistinctDurationFluxTest {


    @Test
    public void test() {

        DistinctDurationFlux
                .create(
                        Flux.create(sink -> {
                                for (int i = 0; i < 1000; i++) {
                                    sink.next(i);
                                }
                                //sink.complete();
                            }),
                        i -> i,
                        Duration.ofMillis(10)
                )
                .concatMap(i -> Mono
                                   .delay(Duration.ofMillis(1))
                                   .thenReturn(i),
                           1)
                .take(1000)
                .timeout(Duration.ofSeconds(2))
                .as(StepVerifier::create)
                .expectNextCount(1000)
                .verifyComplete();

    }
}