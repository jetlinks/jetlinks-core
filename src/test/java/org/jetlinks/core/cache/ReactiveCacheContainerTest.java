package org.jetlinks.core.cache;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ReactiveCacheContainerTest {


    @Test
    public void testNest() {
        ReactiveCacheContainer<String, String> cache = ReactiveCacheContainer.create();

        cache.compute("test", (k, v) -> {
                 return Mono.defer(() -> cache
                                .get("test", Mono.just("x"))
                                .doOnNext(System.out::println))
                            .thenReturn("1");
             })
             //.subscribeOn(Schedulers.parallel())
             .as(StepVerifier::create)
             .expectNext("1")
             .verifyComplete();

    }

    @Test
    public void testError() {
        ReactiveCacheContainer<String, Disposable> cache = ReactiveCacheContainer.create();
        cache
            .compute("test", (k, v) -> Mono
                .error(new RuntimeException("error"))
                .thenReturn(() -> {
                }))
            .as(StepVerifier::create)
            .expectError()
            .verify();

        assertNull(cache.get("test", null));
    }

    @Test
    public void test() {
        ReactiveCacheContainer<String, Disposable> cache = ReactiveCacheContainer.create();
        AtomicBoolean disposed = new AtomicBoolean();
        cache
            .compute("test", (k, v) -> Mono
                .delay(Duration.ofMillis(100))
                .thenReturn(() -> disposed.set(true)))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        cache.remove("test");

        assertTrue(disposed.get());

    }
}