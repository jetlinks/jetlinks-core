package org.jetlinks.core.cache;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ReactiveCacheAtomicOwnershipTest {
    @Test
    public void failedCancellationDoesNotLoseOldCleanupOrLeaveUpdateWaitersPending() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource previous = new Resource(), current = new Resource();
        cache.put("entry", previous);
        var loading = cache.compute("entry", (id, old) -> Mono.<Resource>create(sink -> sink.onCancel(() -> {
            throw new IllegalStateException("cancel failed");
        }))).toFuture();
        var queued = cache.compute("entry", (id, old) -> Mono.just(old)).toFuture();
        assertEquals("cancel failed", expectFailure(() -> cache.put("entry", current)).getMessage());
        assertTrue(loading.isDone());
        assertTrue(queued.isDone());
        assertSame(current, cache.getNow("entry"));
        assertEquals(1, previous.disposals.get());
        cache.clear();
        assertEquals(1, current.disposals.get());
    }

    @Test
    public void failedTakeRetainsTheUnreturnedResourceForCleanup() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource resource = new Resource();
        cache.put("entry", resource);
        var loading = cache.compute("entry", (id, old) -> Mono.<Resource>create(sink -> sink.onCancel(() -> {
            throw new IllegalStateException("cancel failed");
        }))).toFuture();
        expectFailure(() -> cache.take("entry"));
        assertTrue(loading.isDone());
        cache.clear();
        assertEquals(1, resource.disposals.get());
    }

    @Test
    public void failedTakeCannotReclaimAResourceAdoptedByItsCancellationCallback() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource resource = new Resource();
        cache.put("entry", resource);
        cache.compute("entry", (id, old) -> Mono.<Resource>create(sink -> sink.onCancel(() -> {
            cache.put("rescued", resource);
            throw new IllegalStateException("cancel failed");
        }))).toFuture();
        expectFailure(() -> cache.take("entry"));
        assertSame(resource, cache.getNow("rescued"));
        assertEquals(0, resource.disposals.get());
        cache.clear();
        assertEquals(1, resource.disposals.get());
    }

    @Test
    public void cancellationAndReleaseFailuresAreBothReportedAndCleanupRemainsRetryable() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource previous = new Resource(), current = new Resource();
        previous.fail = true;
        cache.put("entry", previous);
        cache.compute("entry", (id, old) -> Mono.<Resource>create(sink -> sink.onCancel(() -> {
            throw new IllegalStateException("cancel failed");
        }))).toFuture();
        RuntimeException failure = expectFailure(() -> cache.put("entry", current));
        assertEquals(2, reactor.core.Exceptions.unwrapMultiple(failure).size());
        assertSame(current, cache.getNow("entry"));
        previous.fail = false;
        cache.clear();
        assertEquals(2, previous.disposals.get());
        assertEquals(1, current.disposals.get());
    }

    @Test
    public void clearAttemptsEveryResourceEvenWhenCancellationFails() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource pending = new Resource(), healthy = new Resource();
        cache.put("pending", pending);
        cache.put("healthy", healthy);
        var loading = cache.compute("pending", (id, old) -> Mono.<Resource>create(sink -> sink.onCancel(() -> {
            throw new IllegalStateException("cancel failed");
        }))).toFuture();
        expectFailure(cache::clear);
        assertTrue(loading.isDone());
        assertEquals(1, pending.disposals.get());
        assertEquals(1, healthy.disposals.get());
    }

    @Test
    public void aLateResultCannotReleaseAValueTransferredFromANewerEntry() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        AtomicReference<MonoSink<Resource>> source = new AtomicReference<>();
        cache.compute("entry", (id, old) -> Mono.create(source::set)).subscribe();
        Resource resource = new Resource();
        cache.put("entry", resource);
        assertSame(resource, cache.take("entry"));
        source.get().success(resource);
        cache.clear();
        assertEquals(0, resource.disposals.get());
        resource.dispose();
    }

    @Test
    public void cancellationCanTransferThePublishedValueBeforeTheLateResultArrives() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource resource = new Resource();
        AtomicReference<Resource> transferred = new AtomicReference<>();
        cache.compute("entry", (id, old) -> Mono.<Resource>create(sink -> sink.onCancel(() -> {
            transferred.set(cache.take("entry"));
            sink.success(resource);
        }))).subscribe();
        cache.put("entry", resource);
        assertSame(resource, transferred.get());
        cache.clear();
        assertEquals(0, resource.disposals.get());
        resource.dispose();
    }

    @Test
    public void aTransferredValueCanBeAdoptedAndReleasedAgain() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource resource = new Resource();
        cache.put("entry", resource);
        assertSame(resource, cache.take("entry"));
        cache.put("entry", resource);
        assertSame(resource, cache.remove("entry"));
        assertEquals(1, resource.disposals.get());
    }

    @Test
    public void aLateResultCannotReleaseAValueMovedToAnotherKey() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        AtomicReference<MonoSink<Resource>> source = new AtomicReference<>();
        cache.compute("entry", (id, old) -> Mono.create(source::set)).subscribe();
        Resource resource = new Resource();
        cache.put("entry", resource);
        assertSame(resource, cache.take("entry"));
        cache.put("moved", resource);
        source.get().success(resource);
        assertSame(resource, cache.getNow("moved"));
        assertEquals(0, resource.disposals.get());
        cache.clear();
        assertEquals(1, resource.disposals.get());
    }

    @Test
    public void transferredOwnershipUsesIdentityInsteadOfEquality() {
        ReactiveCacheContainer<String, Disposable> cache = ReactiveCacheContainer.create();
        class EqualResource implements Disposable {
            int disposals;
            @Override public void dispose() { disposals++; }
            @Override public boolean equals(Object value) { return value instanceof EqualResource; }
            @Override public int hashCode() { return 1; }
        }
        EqualResource transferred = new EqualResource(), current = new EqualResource();
        cache.put("entry", transferred);
        assertSame(transferred, cache.take("entry"));
        cache.put("entry", current);
        cache.clear();
        assertEquals(0, transferred.disposals);
        assertEquals(1, current.disposals);
        transferred.dispose();
    }

    @Test
    public void aLateResultCannotReleaseTheSameValueOwnedByANewEntry() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        AtomicReference<MonoSink<Resource>> source = new AtomicReference<>();
        cache.compute("entry", (id, old) -> Mono.create(source::set)).subscribe();
        Resource current = new Resource();
        cache.put("entry", current);
        source.get().success(current);
        assertSame(current, cache.getNow("entry"));
        assertEquals(0, current.disposals.get());
        cache.clear();
        assertEquals(1, current.disposals.get());
    }

    @Test
    public void aLateResultCannotReleaseAStagedReplacement() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        AtomicReference<MonoSink<Resource>> source = new AtomicReference<>();
        cache.compute("entry", (id, old) -> Mono.create(source::set)).subscribe();
        Resource previous = new Resource(), candidate = new Resource();
        cache.put("entry", previous);
        previous.beforeRelease = () -> source.get().success(candidate);
        cache.compute("entry", (id, old) -> Mono.just(candidate)).as(StepVerifier::create)
            .expectNext(candidate).expectComplete().verify(Duration.ofSeconds(5));
        assertSame(candidate, cache.getNow("entry"));
        assertEquals(0, candidate.disposals.get());
        cache.clear();
        assertEquals(1, candidate.disposals.get());
    }

    @Test
    public void cancellationCallbackCannotReleaseTheValueBeingPublished() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource current = new Resource();
        cache.compute("entry", (id, old) -> Mono.<Resource>create(sink ->
            sink.onCancel(() -> sink.success(current)))).subscribe();
        cache.put("entry", current);
        assertSame(current, cache.getNow("entry"));
        assertEquals(0, current.disposals.get());
        cache.clear();
    }

    @Test
    public void aRejectedTransferPreservesThePreviouslyCommittedOwner() throws Exception {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource retired = new Resource(), current = new Resource();
        retired.fail = true;
        cache.put("entry", retired);
        expectFailure(() -> cache.put("entry", current));
        CountDownLatch releasing = new CountDownLatch(1), finish = new CountDownLatch(1);
        retired.fail = false;
        retired.beforeRelease = () -> { releasing.countDown(); await(finish); };
        AtomicReference<Throwable> failed = new AtomicReference<>();
        Thread retry = new Thread(() -> {
            try { cache.compute("entry", (id, old) -> Mono.just(old)).block(Duration.ofSeconds(5)); }
            catch (Throwable error) { failed.set(error); }
        });
        retry.start();
        try {
            await(releasing);
            assertEquals("Cache resource release is in progress", expectFailure(() -> cache.put("entry", retired)).getMessage());
            assertSame(current, cache.getNow("entry"));
            assertEquals(0, current.disposals.get());
        } finally {
            finish.countDown();
            retry.join(5000);
            cache.clear();
        }
        assertFalse(retry.isAlive());
        assertNull(failed.get());
        assertEquals(1, current.disposals.get());
    }

    @Test
    public void retryHidesTheClaimedValueAndRestoresItOnlyWhenReleaseFails() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource previous = new Resource(), candidate = new Resource();
        previous.fail = true;
        cache.put("entry", previous);
        cache.compute("entry", (id, old) -> Mono.just(candidate)).as(StepVerifier::create)
            .expectErrorMessage("native release failed").verify(Duration.ofSeconds(5));
        previous.beforeRelease = () -> assertNull(cache.getNow("entry"));
        cache.compute("entry", (id, old) -> { throw new AssertionError("factory ran before cleanup"); })
            .as(StepVerifier::create).expectErrorMessage("native release failed").verify(Duration.ofSeconds(5));
        assertSame(previous, cache.getNow("entry"));
        previous.fail = false;
        Resource replacement = new Resource();
        cache.compute("entry", (id, old) -> { assertNull(old); return Mono.just(replacement); }).as(StepVerifier::create)
            .expectNext(replacement).expectComplete().verify(Duration.ofSeconds(5));
        assertEquals(3, previous.disposals.get());
        cache.clear();
    }

    @Test
    public void cancellationCallbacksCanTransferOtherEntriesFromAnotherThread() throws Exception {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource other = new Resource(), current = new Resource();
        cache.put("other", other);
        CountDownLatch transferred = new CountDownLatch(1);
        AtomicReference<Resource> taken = new AtomicReference<>();
        AtomicReference<Thread> callback = new AtomicReference<>();
        AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
        cache.compute("entry", (id, old) -> Mono.<Resource>create(sink -> sink.onCancel(() -> {
            Thread worker = new Thread(() -> { taken.set(cache.take("other")); transferred.countDown(); });
            callback.set(worker);
            worker.start();
            try { await(transferred); }
            catch (Throwable failure) { callbackFailure.set(failure); }
        }))).subscribe();
        try {
            cache.put("entry", current);
            callback.get().join(5000);
            assertNull(callbackFailure.get());
            assertSame(other, taken.get());
            assertEquals(0, other.disposals.get());
        } finally { cache.clear(); other.dispose(); }
    }

    private static RuntimeException expectFailure(Runnable action) {
        try { action.run(); }
        catch (RuntimeException failure) { return failure; }
        throw new AssertionError("Expected resource release failure");
    }

    private static void await(CountDownLatch signal) {
        try { assertTrue("Timed out waiting for signal", signal.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }

    private static final class Resource implements Disposable {
        private final AtomicInteger disposals = new AtomicInteger();
        private volatile boolean fail;
        private Runnable beforeRelease = () -> {};
        @Override public void dispose() {
            disposals.incrementAndGet();
            beforeRelease.run();
            if (fail) throw new IllegalStateException("native release failed");
        }
    }
}
