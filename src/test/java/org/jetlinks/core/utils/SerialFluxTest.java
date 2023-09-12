package org.jetlinks.core.utils;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class SerialFluxTest {


    @Test
    public void test() {
        SerialFlux<Integer> flux = new SerialFlux<>();

        Flux<Integer> first = flux.join(Flux.just(1).delayElements(Duration.ofSeconds(1)));

        Flux<Integer> second = flux
                .join(Flux.create(sink -> {
                    sink.onCancel(() -> {
                        System.out.println("cancel");
                    });
                }));

        Flux<Integer> third = flux.join(Flux.just(2, 3));


        Schedulers.parallel().schedule(() -> {
            second.subscribe().dispose();
        }, 2, TimeUnit.SECONDS);

        Flux.merge(
                    first,
                    third
            )
            .doOnNext(System.out::println)
            .as(StepVerifier::create)
            .expectNext(1, 2, 3)
            .verifyComplete();

    }

}