package org.jetlinks.core.cache;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ReactiveCacheUpdateLifecycleTest {
    @BeforeClass public static void boundVerification() { StepVerifier.setDefaultTimeout(Duration.ofSeconds(5)); }
    @AfterClass public static void resetVerification() { StepVerifier.resetDefaultTimeout(); }

    @Test
    public void failedUpdateRetainsTheLoadedOwnerForCleanupAndRetry() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource original = new Resource();
        cache.put("entry", original);
        cache.compute("entry", (id, old) -> Mono.error(new IllegalStateException("reload failed")))
            .as(StepVerifier::create).expectErrorMessage("reload failed").verify();
        assertSame(original, cache.getNow("entry"));
        assertSame(original, cache.get("entry", Mono.empty()).block());
        assertSame(original, cache.take("entry"));
        assertEquals(0, original.disposals.get());
        original.dispose();
    }

    @Test
    public void updatesWaitForTheirImmediatePredecessorAndReturnTheirOwnResult() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        Sinks.One<Integer> first = Sinks.one(), second = Sinks.one(), third = Sinks.one();
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<Integer> a = cache.compute("entry", (id, old) -> first.asMono()).toFuture();
        CompletableFuture<Integer> b = cache.compute("entry", (id, old) -> {
            assertEquals(Integer.valueOf(1), old); calls.incrementAndGet(); return second.asMono();
        }).toFuture();
        CompletableFuture<Integer> c = cache.compute("entry", (id, old) -> {
            assertEquals(Integer.valueOf(2), old); calls.incrementAndGet(); return third.asMono();
        }).toFuture();
        try {
            first.tryEmitValue(1);
            assertEquals(1, calls.get());
            assertEquals(Integer.valueOf(1), a.join());
            second.tryEmitValue(2);
            assertEquals(2, calls.get());
            assertEquals(Integer.valueOf(2), b.join());
            third.tryEmitValue(3);
            assertEquals(Integer.valueOf(3), c.join());
        } finally {
            first.tryEmitValue(1); second.tryEmitValue(2); third.tryEmitValue(3);
            cache.dispose();
        }
    }

    @Test
    public void anUpdateFailureDoesNotCancelAQueuedHealthyUpdate() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        cache.put("entry", 1);
        Sinks.One<Integer> failed = Sinks.one();
        AtomicReference<Throwable> error = new AtomicReference<>();
        cache.compute("entry", (id, old) -> failed.asMono()).subscribe(value -> fail(), error::set);
        CompletableFuture<Integer> next = cache.compute("entry", (id, old) -> Mono.just(old + 1)).toFuture();
        failed.tryEmitError(new IllegalStateException("failed"));
        assertNotNull(error.get());
        assertEquals(Integer.valueOf(2), next.join());
        assertEquals(Integer.valueOf(2), cache.getNow("entry"));
    }

    @Test
    public void concurrentUpdatesDoNotLoseAnyIncrement() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        List<Integer> results = Flux.range(0, 100)
            .flatMap(i -> cache.compute("entry", (id, old) -> Mono.just(old == null ? 1 : old + 1))
                .subscribeOn(Schedulers.parallel()), 16)
            .collectList().block(Duration.ofSeconds(5));
        assertNotNull(results);
        assertEquals(100, results.stream().distinct().count());
        assertEquals(Integer.valueOf(100), cache.getNow("entry"));
    }

    @Test
    public void failedInitialLoadIsEvictedAndANewRequestCanRetry() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        cache.computeIfAbsent("entry", id -> Mono.<Integer>error(new IllegalStateException("failed")))
            .as(StepVerifier::create).expectError(IllegalStateException.class).verify();
        assertFalse(cache.containsKey("entry"));
        cache.computeIfAbsent("entry", id -> Mono.just(1)).as(StepVerifier::create).expectNext(1).verifyComplete();
    }

    @Test
    public void concurrentAbsentLoadsShareTheFirstSuccessfulValue() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        Sinks.One<Integer> source = Sinks.one();
        AtomicInteger calls = new AtomicInteger();
        var a = cache.computeIfAbsent("entry", id -> { calls.incrementAndGet(); return source.asMono(); }).toFuture();
        var b = cache.computeIfAbsent("entry", id -> { calls.incrementAndGet(); return Mono.just(2); }).toFuture();
        source.tryEmitValue(1);
        assertEquals(Integer.valueOf(1), a.join());
        assertEquals(Integer.valueOf(1), b.join());
        assertEquals(1, calls.get());
    }

    @Test
    public void takeCancelsQueuedUpdatesWithoutDisposingTheTransferredValue() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource original = new Resource(); cache.put("entry", original);
        AtomicInteger queued = new AtomicInteger();
        var first = cache.compute("entry", (id, old) -> Mono.<Resource>never()).toFuture();
        var second = cache.compute("entry", (id, old) -> { queued.incrementAndGet(); return Mono.just(old); }).toFuture();
        assertSame(original, cache.take("entry"));
        assertNull(first.join()); assertNull(second.join());
        assertEquals(0, queued.get()); assertEquals(0, original.disposals.get());
        original.dispose();
    }

    @Test
    public void emptyUpdatesKeepTheLoadedOwnerUntilExplicitRemoval() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource original = new Resource(); cache.put("entry", original);
        cache.compute("entry", (id, old) -> Mono.empty()).as(StepVerifier::create).verifyComplete();
        assertSame(original, cache.remove("entry"));
        assertEquals(1, original.disposals.get());
    }

    @Test
    public void reusingAViewDoesNotReplayARetiredValueOrRepeatTheUpdate() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        AtomicInteger calls = new AtomicInteger();
        Mono<Integer> view = cache.compute("entry", (id, old) -> Mono.just(calls.incrementAndGet()));
        assertFalse(cache.containsKey("entry"));
        assertEquals(Integer.valueOf(1), view.block());
        cache.compute("entry", (id, old) -> Mono.just(2)).block();
        assertEquals(Integer.valueOf(2), view.block());
        assertEquals(1, calls.get());
        cache.take("entry");
        view.as(StepVerifier::create).verifyComplete();
    }

    @Test
    public void manyReadersShareOnePendingAbsentLoadWithoutFillingTheUpdateQueue() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        Sinks.One<Integer> source = Sinks.one();
        AtomicInteger calls = new AtomicInteger();
        var readers = Flux.range(0, 1000).flatMap(i -> cache.computeIfAbsent("entry", id -> {
            calls.incrementAndGet(); return source.asMono();
        }), 1000).collectList().toFuture();
        source.tryEmitValue(1);
        assertEquals(1000, readers.join().size());
        assertEquals(1, calls.get());
    }

    @Test
    public void inFlightReadersSeeTheFailureAndLaterReadsKeepTheOldOwner() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        cache.put("entry", 1);
        Sinks.One<Integer> source = Sinks.one();
        AtomicReference<Throwable> updateError = new AtomicReference<>(), readError = new AtomicReference<>();
        cache.compute("entry", (id, old) -> source.asMono()).subscribe(value -> fail(), updateError::set);
        cache.get("entry", Mono.empty()).subscribe(value -> fail(), readError::set);
        source.tryEmitError(new IllegalStateException("failed"));
        assertNotNull(updateError.get());
        assertSame(updateError.get(), readError.get());
        assertEquals(Integer.valueOf(1), cache.get("entry", Mono.empty()).block());
    }

    @Test
    public void recursiveAbsentLookupUsesTheCurrentSnapshotWithoutWaitingOnItself() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        cache.compute("entry", (id, old) -> cache.computeIfAbsent("entry", ignored -> {
            throw new AssertionError("recursive factory must not run");
        }).defaultIfEmpty(1)).as(StepVerifier::create).expectNext(1).verifyComplete();
    }

    @Test
    public void retryingAFailedUpdateViewCannotReportTheOldValueAsASuccess() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        cache.put("entry", 1);
        AtomicInteger calls = new AtomicInteger();
        cache.compute("entry", (id, old) -> {
            calls.incrementAndGet(); return Mono.<Integer>error(new IllegalStateException("failed"));
        }).retry(1).as(StepVerifier::create).expectErrorMessage("failed").verify();
        assertEquals(1, calls.get());
        assertEquals(Integer.valueOf(1), cache.get("entry", Mono.empty()).block());
    }

    @Test
    public void concurrentSubscriptionsToOneViewAcceptItsUpdateOnlyOnce() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        AtomicInteger calls = new AtomicInteger();
        Mono<Integer> view = cache.compute("entry", (id, old) -> Mono.just(calls.incrementAndGet()));
        var values = Flux.range(0, 1000).flatMap(i -> view.subscribeOn(Schedulers.parallel()), 64)
            .collectList().block(Duration.ofSeconds(5));
        assertNotNull(values);
        assertEquals(1000, values.size());
        assertTrue(values.stream().allMatch(value -> value == 1));
        assertEquals(1, calls.get());
    }

    @Test
    public void filteringAnExistingValueDoesNotDiscardTheCachesBorrowedOwner() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource original = new Resource(); cache.put("entry", original);
        cache.compute("entry", (id, old) -> Mono.just(old).filter(value -> false))
            .as(StepVerifier::create).verifyComplete();
        assertSame(original, cache.getNow("entry"));
        assertEquals(0, original.disposals.get());
        cache.remove("entry");
        assertEquals(1, original.disposals.get());
    }

    @Test
    public void puttingTheSameResourceDoesNotDisposeTheNewOwner() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource original = new Resource(); cache.put("entry", original);
        assertSame(original, cache.put("entry", original));
        assertSame(original, cache.getNow("entry"));
        assertEquals(0, original.disposals.get());
        cache.remove("entry");
        assertEquals(1, original.disposals.get());
    }

    private static final class Resource implements Disposable {
        final AtomicInteger disposals = new AtomicInteger();
        @Override public void dispose() { disposals.incrementAndGet(); }
    }
}
