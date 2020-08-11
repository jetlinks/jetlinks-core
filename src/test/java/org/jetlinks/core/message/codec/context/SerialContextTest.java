package org.jetlinks.core.message.codec.context;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;


public class SerialContextTest {


    @Test
    public void test() {
        SerialContext<Integer, String> ctx = SerialContext.newContext();

        ctx.listen()
                .map(String::valueOf)
                .delayElements(Duration.ofMillis(500))
                .doOnNext(ctx::output)
                .subscribe();

        Flux.range(1, 5)
                .publishOn(Schedulers.parallel())
                .flatMap(in -> ctx.inputAndAwait(in, Duration.ofSeconds(10)))
                .doOnNext(System.out::println)
                .as(StepVerifier::create)
                .expectNext("1","2","3","4","5")
                .verifyComplete();

    }

}