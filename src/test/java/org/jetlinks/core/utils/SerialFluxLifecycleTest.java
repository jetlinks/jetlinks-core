package org.jetlinks.core.utils;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SerialFluxLifecycleTest {
    @BeforeClass
    public static void boundVerification() { StepVerifier.setDefaultTimeout(Duration.ofSeconds(5)); }

    @AfterClass
    public static void resetVerification() { StepVerifier.resetDefaultTimeout(); }

    @Test
    public void unsubscribedWorkDoesNotRunOrLoseItsResult() {
        SerialFlux<Integer> serial = new SerialFlux<>();
        AtomicInteger calls = new AtomicInteger();
        Flux<Integer> first = serial.join(Flux.defer(() -> Flux.just(calls.incrementAndGet())));
        serial.join(Flux.just(2)).as(StepVerifier::create).expectNext(2).verifyComplete();
        assertEquals(0, calls.get());
        first.as(StepVerifier::create).expectNext(1).verifyComplete();
    }

    @Test
    public void forwardsLaterDemandAndDoesNotRequestWithoutDemand() {
        SerialFlux<Integer> serial = new SerialFlux<>();
        AtomicInteger produced = new AtomicInteger();
        serial.join(Flux.range(1, 3).doOnNext(value -> produced.incrementAndGet()))
            .as(flux -> StepVerifier.create(flux, 0))
            .then(() -> assertEquals(0, produced.get()))
            .thenRequest(1).expectNext(1)
            .thenRequest(2).expectNext(2, 3).verifyComplete();
    }

    @Test
    public void queuedDemandAccumulatesBeforeTheUpstreamIsSubscribed() {
        SerialFlux<Integer> serial = new SerialFlux<>();
        Sinks.Empty<Void> gate = Sinks.empty();
        Disposable first = serial.join(gate.asMono().thenMany(Flux.empty())).subscribe();
        try {
            serial.join(Flux.range(1, 3)).as(flux -> StepVerifier.create(flux, 0))
                .thenRequest(1).thenRequest(2)
                .then(gate::tryEmitEmpty).expectNext(1, 2, 3).verifyComplete();
        } finally {
            gate.tryEmitEmpty();
            first.dispose();
        }
    }

    @Test
    public void cancellationInOnSubscribeNeverAcceptsOrExecutesWork() {
        SerialFlux<Integer> serial = new SerialFlux<>();
        AtomicInteger calls = new AtomicInteger();
        serial.join(Flux.defer(() -> Flux.just(calls.incrementAndGet())))
            .subscribe(new BaseSubscriber<Integer>() {
                @Override protected void hookOnSubscribe(Subscription subscription) { cancel(); }
            });
        serial.join(Flux.just(2)).as(StepVerifier::create).expectNext(2).verifyComplete();
        assertEquals(0, calls.get());
        assertEquals(0, serial.size());
    }

    @Test
    public void cancelledQueuedWorkIsSkippedAndActiveCancellationReleasesTheNextWork() {
        SerialFlux<Integer> serial = new SerialFlux<>();
        Disposable first = serial.join(Flux.never()).subscribe();
        AtomicInteger cancelledCalls = new AtomicInteger();
        Disposable cancelled = serial.join(Flux.defer(() -> Flux.just(cancelledCalls.incrementAndGet()))).subscribe();
        cancelled.dispose();
        serial.join(Flux.just(3)).as(StepVerifier::create)
            .then(first::dispose).expectNext(3).verifyComplete();
        assertEquals(0, cancelledCalls.get());
        assertEquals(0, serial.size());
    }

    @Test
    public void errorReleasesTheQueueAndEachSubscriptionKeepsItsContext() {
        SerialFlux<String> serial = new SerialFlux<>();
        serial.join(Flux.error(new IllegalStateException("failed"))).as(StepVerifier::create)
            .expectError(IllegalStateException.class).verify();
        serial.join(Flux.deferContextual(ctx -> Flux.just(ctx.<String>get("identity"))))
            .contextWrite(Context.of("identity", "next"))
            .as(StepVerifier::create).expectNext("next").verifyComplete();
    }

    @Test
    public void synchronousQueuedCompletionsDoNotRecurse() {
        SerialFlux<Integer> serial = new SerialFlux<>(ConcurrentLinkedQueue::new);
        Sinks.Empty<Void> gate = Sinks.empty();
        serial.join(gate.asMono().thenMany(Flux.empty())).subscribe();
        AtomicInteger completed = new AtomicInteger();
        for (int i = 0; i < 10000; i++) {
            serial.join(Flux.just(i)).doOnComplete(completed::incrementAndGet).subscribe();
        }
        gate.tryEmitEmpty();
        assertEquals(10000, completed.get());
        assertEquals(0, serial.size());
    }

    @Test
    public void concurrentProducersNeverOverlapTheirSources() throws Exception {
        SerialFlux<Integer> serial = new SerialFlux<>(ConcurrentLinkedQueue::new);
        Sinks.Empty<Void> gate = Sinks.empty();
        serial.join(gate.asMono().thenMany(Flux.empty())).subscribe();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger overlap = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        CountDownLatch producers = new CountDownLatch(4);
        for (int i = 0; i < 4; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        serial.join(Flux.defer(() -> {
                            if (active.incrementAndGet() != 1) overlap.incrementAndGet();
                            return Flux.just(1).doOnComplete(active::decrementAndGet);
                        })).doOnComplete(completed::incrementAndGet).subscribe();
                    }
                } finally { producers.countDown(); }
            }).start();
        }
        assertTrue(producers.await(5, TimeUnit.SECONDS));
        gate.tryEmitEmpty();
        assertEquals(400, completed.get());
        assertEquals(0, overlap.get());
        assertEquals(0, serial.size());
    }

    @Test
    public void aSecondSubscriberIsRejectedWithoutAffectingTheFirst() {
        SerialFlux<Integer> serial = new SerialFlux<>();
        Sinks.One<Integer> value = Sinks.one();
        Flux<Integer> request = serial.join(value.asMono().flux());
        request.as(StepVerifier::create)
            .then(() -> request.as(StepVerifier::create).expectError(IllegalStateException.class).verify())
            .then(() -> value.tryEmitValue(1)).expectNext(1).verifyComplete();
    }

    @Test
    public void queueOverflowFailsOnlyTheRejectedWorkAndRetainsAcceptedWork() {
        SerialFlux<Integer> serial = new SerialFlux<>(() -> new ArrayBlockingQueue<>(1));
        Disposable first = serial.join(Flux.never()).subscribe();
        try {
            serial.join(Flux.just(2)).as(StepVerifier::create)
                .then(() -> serial.join(Flux.just(3)).as(StepVerifier::create)
                    .expectErrorMessage("pending queue is full").verify())
                .then(first::dispose).expectNext(2).verifyComplete();
            assertEquals(0, serial.size());
        } finally {
            first.dispose();
        }
    }
}
