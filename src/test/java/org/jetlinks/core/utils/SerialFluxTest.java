package org.jetlinks.core.utils;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class SerialFluxTest {


    @Test
    public void test() {
        SerialFlux<Integer> flux = new SerialFlux<>();

        Sinks.Many<Integer> sink = Sinks.many().multicast().directAllOrNothing();

        Schedulers
                .parallel()
                .schedule(() -> {
                    System.out.println("complete");
                    sink.tryEmitComplete();
                }, 5, TimeUnit.SECONDS);

        Flux.merge(
                    flux.join(Flux.just(1).delayElements(Duration.ofSeconds(2))),
                    flux.join(sink.asFlux()),
                    flux.join(Flux.just(2, 3).delayElements(Duration.ofSeconds(1)))
            )
            .doOnNext(System.out::println)
            .as(StepVerifier::create)
            .expectNext(1, 2, 3)
            .verifyComplete();

    }

}