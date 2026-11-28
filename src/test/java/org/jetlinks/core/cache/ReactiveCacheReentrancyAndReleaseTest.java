package org.jetlinks.core.cache;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ReactiveCacheReentrancyAndReleaseTest {
    @BeforeClass public static void boundVerification() { StepVerifier.setDefaultTimeout(Duration.ofSeconds(2)); }
    @AfterClass public static void resetVerification() { StepVerifier.resetDefaultTimeout(); }

    @Test
    public void nestedKeysReadTheLoadedAncestorInsteadOfWaitingForItsUpdate() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        cache.put("a", 1);
        try {
            cache.compute("a", (id, old) -> cache.compute("b", (key, previous) -> cache.get("a", Mono.empty())))
                .as(StepVerifier::create).expectNext(1).verifyComplete();
        } finally { cache.clear(); }
    }

    @Test
    public void nestedContainersKeepEachOthersLoadingAncestors() {
        ReactiveCacheContainer<String, Integer> a = ReactiveCacheContainer.create(), b = ReactiveCacheContainer.create();
        a.put("a", 1);
        try {
            a.compute("a", (id, old) -> b.compute("b", (key, previous) -> a.get("a", Mono.empty())))
                .as(StepVerifier::create).expectNext(1).verifyComplete();
        } finally { a.clear(); b.clear(); }
    }

    @Test
    public void anUnloadedAncestorUsesTheExplicitFallbackWithoutReenteringItsFactory() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        try {
            cache.compute("a", (id, old) -> cache.compute("b", (key, previous) ->
                    cache.computeIfAbsent("a", ignored -> { throw new AssertionError("recursive factory"); }).defaultIfEmpty(3)))
                .as(StepVerifier::create).expectNext(3).verifyComplete();
        } finally { cache.clear(); }
    }

    @Test
    public void reusingTheLoadingUpdateViewReadsItsCurrentSnapshot() {
        ReactiveCacheContainer<String, Integer> cache = ReactiveCacheContainer.create();
        cache.put("entry", 1);
        AtomicReference<Mono<Integer>> view = new AtomicReference<>();
        view.set(cache.compute("entry", (id, old) -> Mono.defer(view::get)));
        try {
            view.get().as(StepVerifier::create).expectNext(1).verifyComplete();
        } finally { cache.clear(); }
    }

    @Test
    public void failedReplacementRetainsThePreviousOwnerAndReleasesTheUncommittedValue() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource previous = new Resource(true), replacement = new Resource(false);
        cache.put("entry", previous);
        cache.compute("entry", (id, old) -> Mono.just(replacement)).as(StepVerifier::create)
            .expectErrorMessage("native release failed").verify();
        assertSame(previous, cache.getNow("entry"));
        assertTrue(previous.active);
        assertFalse(replacement.active);
        assertEquals(1, replacement.disposals.get());
        previous.fail = false;
        assertSame(previous, cache.remove("entry"));
        assertFalse(previous.active);
        assertEquals(2, previous.disposals.get());
    }

    @Test
    public void clearAttemptsEveryResourceAndKeepsFailuresForTargetedAndFullRetry() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource first = new Resource(true), second = new Resource(true), healthy = new Resource(false);
        cache.put("first", first); cache.put("second", second); cache.put("healthy", healthy);
        expectFailure(cache::clear);
        assertEquals(1, first.disposals.get()); assertEquals(1, second.disposals.get());
        assertFalse(healthy.active);
        first.fail = false;
        assertNull(cache.remove("first"));
        assertFalse(first.active);
        assertTrue(second.active);
        second.fail = false;
        cache.clear();
        assertFalse(second.active);
        assertEquals(2, first.disposals.get()); assertEquals(2, second.disposals.get());
        assertEquals(1, healthy.disposals.get());
    }

    @Test
    public void failedRemovalCanBeRetriedEvenAfterTheEntryWasDetached() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource resource = new Resource(true); cache.put("entry", resource);
        expectFailure(() -> cache.remove("entry"));
        resource.fail = false;
        assertNull(cache.remove("entry"));
        assertFalse(resource.active);
        assertEquals(2, resource.disposals.get());
    }

    @Test
    public void putRetainsBothTheCommittedValueAndFailedPreviousCleanup() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource previous = new Resource(true), replacement = new Resource(false);
        cache.put("entry", previous);
        expectFailure(() -> cache.put("entry", replacement));
        assertSame(replacement, cache.getNow("entry"));
        previous.fail = false;
        assertSame(replacement, cache.remove("entry"));
        assertFalse(previous.active); assertFalse(replacement.active);
    }

    @Test
    public void failedCandidateCleanupRemainsOwnedAfterThePreviousValueIsTransferred() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource previous = new Resource(true), replacement = new Resource(true);
        cache.put("entry", previous);
        cache.compute("entry", (id, old) -> Mono.just(replacement)).as(StepVerifier::create).expectError().verify();
        assertEquals(1, replacement.disposals.get());
        assertSame(previous, cache.take("entry"));
        replacement.fail = false;
        assertNull(cache.remove("entry"));
        assertFalse(replacement.active);
        assertEquals(1, previous.disposals.get());
        previous.fail = false; previous.dispose();
    }

    @Test
    public void takeDuringReplacementCannotTransferAValueAlreadyClaimedForRelease() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource previous = new Resource(false), replacement = new Resource(false);
        cache.put("entry", previous);
        previous.beforeRelease = () -> assertNull(cache.take("entry"));
        cache.compute("entry", (id, old) -> Mono.just(replacement)).as(StepVerifier::create).verifyComplete();
        assertFalse(previous.active); assertFalse(replacement.active);
        assertNull(cache.getNow("entry"));
    }

    @Test
    public void aNewLoadMustFinishRetiredCleanupBeforeCallingItsFactory() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource previous = new Resource(true), replacement = new Resource(false);
        cache.put("entry", previous);
        expectFailure(() -> cache.remove("entry"));
        AtomicInteger calls = new AtomicInteger();
        cache.computeIfAbsent("entry", id -> { calls.incrementAndGet(); return Mono.just(replacement); })
            .as(StepVerifier::create).expectErrorMessage("native release failed").verify();
        assertEquals(0, calls.get());
        previous.fail = false;
        cache.computeIfAbsent("entry", id -> { calls.incrementAndGet(); return Mono.just(replacement); })
            .as(StepVerifier::create).expectNext(replacement).verifyComplete();
        assertEquals(1, calls.get());
        cache.clear();
    }

    @Test
    public void clearDoesNotRetryResourcesCreatedByAConcurrentReplacementAfterItsSnapshot() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource trigger = new Resource(false), previous = new Resource(true), candidate = new Resource(false);
        cache.put("trigger", trigger);
        trigger.beforeRelease = () -> {
            cache.put("new", previous);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            cache.compute("new", (id, old) -> Mono.just(candidate)).subscribe(value -> fail(), failure::set);
            assertNotNull(failure.get());
        };
        cache.clear();
        assertSame(previous, cache.getNow("new"));
        assertEquals(1, previous.disposals.get());
        previous.fail = false;
        cache.remove("new");
    }

    @Test
    public void aRetiredSnapshotCannotDisposeResourcesTransferredByAnEarlierCleanupCallback() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        List<String> keys = List.of("a", "b", "c");
        List<Resource> resources = new ArrayList<>(), transferred = new ArrayList<>();
        for (String key : keys) {
            Resource previous = new Resource(true);
            resources.add(previous);
            cache.put(key, previous);
            cache.compute(key, (id, old) -> Mono.just(new Resource(false)))
                .as(StepVerifier::create).expectErrorMessage("native release failed").verify();
            previous.fail = false;
            previous.beforeRelease = () -> {
                for (String other : keys) {
                    if (other.equals(key)) continue;
                    Resource value = cache.take(other);
                    if (value != null) transferred.add(value);
                }
            };
        }
        cache.clear();
        assertEquals(2, transferred.size());
        for (Resource resource : transferred) {
            assertTrue(resource.active);
            assertEquals(1, resource.disposals.get());
        }
        cache.clear();
        for (Resource resource : transferred) resource.dispose();
        assertTrue(resources.stream().noneMatch(value -> value.active));
    }

    private static RuntimeException expectFailure(Runnable action) {
        try { action.run(); }
        catch (RuntimeException failure) { return failure; }
        throw new AssertionError("Expected resource release failure");
    }

    private static final class Resource implements Disposable {
        private final AtomicInteger disposals = new AtomicInteger();
        private boolean active = true;
        private boolean fail;
        private Runnable beforeRelease = () -> {};
        private Resource(boolean fail) { this.fail = fail; }
        @Override public void dispose() {
            disposals.incrementAndGet();
            beforeRelease.run();
            if (fail) throw new IllegalStateException("native release failed");
            active = false;
        }
    }
}
