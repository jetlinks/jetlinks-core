package org.jetlinks.core.utils;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SerialFluxContractTest {
    @Test
    public void unsubscribedOperationDoesNotBlockAndContextAndDemandArePreserved() {
        var serial = new SerialFlux<String>(2);
        serial.join(Flux.never());
        StepVerifier.create(serial.join(Flux.deferContextual(ctx -> Flux.just(ctx.<String>get("key"))))
            .contextWrite(Context.of("key", "value")), 0)
            .thenRequest(1).expectNext("value").verifyComplete();
        assertEquals(0, serial.size());
    }

    @Test
    public void cancellingWaiterDoesNotReleaseActiveOperationAndOverflowDoesNotRunInParallel() {
        var serial = new SerialFlux<Integer>(1);
        Sinks.One<Integer> first = Sinks.one();
        List<Integer> values = new ArrayList<>();
        serial.join(first.asMono().flux()).subscribe(values::add);
        var waiting = serial.join(Flux.just(2)).subscribe(values::add);
        StepVerifier.create(serial.join(Flux.just(3))).expectError(RejectedExecutionException.class).verify();
        waiting.dispose();
        serial.join(Flux.just(4)).subscribe(values::add);
        assertTrue(values.isEmpty());
        first.tryEmitValue(1);
        assertEquals(List.of(1, 4), values);
        assertEquals(0, serial.size());
    }

    @Test
    public void disposalCompletesWaitersWithCancellationAndCancelsSource() {
        var serial = new SerialFlux<Integer>(2);
        AtomicInteger cancelled = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        serial.join(Flux.<Integer>never().doOnCancel(cancelled::incrementAndGet))
            .subscribe(ignore -> fail(), error -> { assertTrue(error instanceof CancellationException); errors.incrementAndGet(); });
        serial.join(Flux.just(2)).subscribe(ignore -> fail(), error -> errors.incrementAndGet());
        serial.dispose();
        assertEquals(1, cancelled.get());
        assertEquals(2, errors.get());
        assertEquals(0, serial.size());
    }

    @Test
    public void cancellationSupportsSuppliedQueuesWithoutArbitraryRemoval() {
        var serial = new SerialFlux<Integer>(reactor.util.concurrent.Queues.small());
        Sinks.One<Integer> first = Sinks.one();
        List<Integer> values = new ArrayList<>();
        try {
            serial.join(first.asMono().flux()).subscribe(values::add);
            serial.join(Flux.just(2)).subscribe(values::add);
            var cancelled = serial.join(Flux.just(3)).subscribe(values::add);
            assertEquals(2, serial.size());
            cancelled.dispose();
            assertEquals(1, serial.size());
            serial.join(Flux.just(4)).subscribe(values::add);
            assertTrue(values.isEmpty());
            first.tryEmitValue(1).orThrow();
            assertEquals(List.of(1, 2, 4), values);
            assertEquals(0, serial.size());
        } finally {
            serial.dispose();
        }
    }

    @Test
    public void concurrentPublishersNeverOverlapTheirBusinessStage() {
        var serial = new SerialFlux<Integer>(256);
        AtomicInteger active = new AtomicInteger(), maximum = new AtomicInteger();
        StepVerifier.create(Flux.range(0, 2000).flatMap(i -> serial.join(Mono.fromSupplier(() -> {
            maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
            return i;
        }).doOnSuccess(ignore -> active.decrementAndGet()).subscribeOn(Schedulers.parallel()).flux()), 128))
            .expectNextCount(2000).verifyComplete();
        assertEquals(1, maximum.get());
    }

    @Test
    public void disposalRacingCompletionHasExactlyOneTerminalSignal() {
        for (int i = 0; i < 200; i++) {
            var serial = new SerialFlux<Integer>(1);
            var source = Sinks.<Integer>one();
            var terminal = new AtomicInteger();
            serial.join(source.asMono().flux()).subscribe(ignore -> {}, error -> terminal.incrementAndGet(), terminal::incrementAndGet);
            var start = new CountDownLatch(1);
            var completed = CompletableFuture.runAsync(() -> {
                try { start.await(); } catch (InterruptedException e) { throw new RuntimeException(e); }
                source.tryEmitEmpty();
            });
            start.countDown();
            serial.dispose();
            completed.join();
            assertEquals(1, terminal.get());
        }
    }
}
