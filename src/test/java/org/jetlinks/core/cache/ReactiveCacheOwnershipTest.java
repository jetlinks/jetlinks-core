package org.jetlinks.core.cache;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ReactiveCacheOwnershipTest {
    @Test
    public void takeTransfersTheLoadedValueWithoutDisposingIt() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource resource = new Resource();
        cache.put("entry", resource);
        assertSame(resource, cache.take("entry"));
        assertNull(cache.getNow("entry"));
        assertEquals(0, resource.disposals.get());
        resource.dispose();
        assertEquals(1, resource.disposals.get());
    }

    @Test
    public void removeStillDisposesAndReturnsTheRemovedValue() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource resource = new Resource();
        cache.put("entry", resource);
        assertSame(resource, cache.remove("entry"));
        assertEquals(1, resource.disposals.get());
    }

    @Test
    public void takeCancelsPendingLoadsAndCompletesTheirWaiters() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        cache.compute("entry", (key, old) -> Mono.<Resource>never().doOnCancel(() -> cancelled.set(true)))
            .subscribe(value -> fail("Detached load must not return a value"), error -> fail(error.toString()), () -> completed.set(true));
        assertNull(cache.take("entry"));
        assertTrue(cancelled.get());
        assertTrue(completed.get());
    }

    @Test
    public void lateResultsCannotReplaceANewerEntryAndAreDisposed() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        AtomicReference<MonoSink<Resource>> source = new AtomicReference<>();
        cache.compute("entry", (key, old) -> Mono.<Resource>create(source::set)).subscribe();
        cache.take("entry");
        Resource replacement = new Resource();
        cache.put("entry", replacement);
        Resource late = new Resource();
        source.get().success(late);
        assertSame(replacement, cache.getNow("entry"));
        assertEquals(1, late.disposals.get());
        assertEquals(0, replacement.disposals.get());
        cache.dispose();
    }

    @Test
    public void readersCapturedBeforeTakeCannotReadTheDetachedValue() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource resource = new Resource();
        cache.put("entry", resource);
        Mono<Resource> previous = cache.get("entry", Mono.empty());
        assertSame(resource, cache.take("entry"));
        previous.as(StepVerifier::create).verifyComplete();
        resource.dispose();
    }

    @Test
    public void takingAnEntryDuringReloadTransfersThePreviousOwnerAndDisposesTheLateReplacement() {
        ReactiveCacheContainer<String, Resource> cache = ReactiveCacheContainer.create();
        Resource original = new Resource();
        cache.put("entry", original);
        AtomicReference<MonoSink<Resource>> replacement = new AtomicReference<>();
        cache.compute("entry", (key, old) -> {
            assertSame(original, old);
            return Mono.<Resource>create(replacement::set);
        }).subscribe();
        assertSame(original, cache.take("entry"));
        assertEquals(0, original.disposals.get());
        Resource late = new Resource();
        replacement.get().success(late);
        assertEquals(1, late.disposals.get());
        assertNull(cache.getNow("entry"));
        original.dispose();
    }

    private static final class Resource implements Disposable {
        final AtomicInteger disposals = new AtomicInteger();
        @Override public void dispose() { disposals.incrementAndGet(); }
    }
}
