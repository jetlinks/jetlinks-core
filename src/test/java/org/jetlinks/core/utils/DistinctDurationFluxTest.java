package org.jetlinks.core.utils;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DistinctDurationFluxTest {

    @Test
    public void shouldCompleteEmptySource() {
        StepVerifier
            .create(DistinctDurationFlux.create(Flux.empty(), Function.identity(), Duration.ofSeconds(1)))
            .verifyComplete();
    }

    @Test
    public void shouldAlwaysPassNullKeys() {
        AtomicLong ticker = new AtomicLong();

        StepVerifier
            .create(DistinctDurationFlux.create(
                Flux.just(1, 2, 3),
                ignored -> null,
                Duration.ofNanos(10),
                ticker::get))
            .expectNext(1, 2, 3)
            .verifyComplete();
    }

    @Test
    public void shouldUseFixedWindowAndRebuildWindowAfterExpiry() {
        AtomicLong ticker = new AtomicLong();
        Flux<TimedValue> source = Flux
            .just(
                new TimedValue("first", "key", 0),
                new TimedValue("suppressed", "key", 9),
                new TimedValue("expired", "key", 10),
                new TimedValue("suppressed-again", "key", 10))
            .doOnNext(value -> ticker.set(value.time));

        StepVerifier
            .create(DistinctDurationFlux.create(
                source,
                value -> value.key,
                Duration.ofNanos(10),
                ticker::get))
            .expectNextMatches(value -> value.name.equals("first"))
            .expectNextMatches(value -> value.name.equals("expired"))
            .verifyComplete();
    }

    @Test
    public void shouldMaintainIndependentWindowsForMultipleKeys() {
        AtomicLong ticker = new AtomicLong();
        Flux<TimedValue> source = Flux
            .just(
                new TimedValue("a0", "a", 0),
                new TimedValue("b2", "b", 2),
                new TimedValue("a5", "a", 5),
                new TimedValue("a10", "a", 10),
                new TimedValue("b11", "b", 11),
                new TimedValue("b12", "b", 12))
            .doOnNext(value -> ticker.set(value.time));

        StepVerifier
            .create(DistinctDurationFlux.create(
                source,
                value -> value.key,
                Duration.ofNanos(10),
                ticker::get))
            .expectNextMatches(value -> value.name.equals("a0"))
            .expectNextMatches(value -> value.name.equals("b2"))
            .expectNextMatches(value -> value.name.equals("a10"))
            .expectNextMatches(value -> value.name.equals("b12"))
            .verifyComplete();
    }

    @Test
    public void shouldCreateIndependentStateForEverySubscription() {
        Flux<Integer> distinct = DistinctDurationFlux.create(
            Flux.just(1, 1),
            Function.identity(),
            Duration.ofSeconds(1),
            () -> 0L);

        StepVerifier.create(distinct).expectNext(1).verifyComplete();
        StepVerifier.create(distinct).expectNext(1).verifyComplete();
    }

    @Test
    public void shouldCreateFreshStateWhenRetryResubscribes() {
        AtomicInteger subscriptions = new AtomicInteger();
        Flux<Integer> source = Flux.defer(() -> subscriptions.incrementAndGet() == 1
            ? Flux.concat(Flux.just(1), Flux.error(new IllegalStateException("retry")))
            : Flux.just(1));

        StepVerifier
            .create(DistinctDurationFlux
                .create(source, Function.identity(), Duration.ofSeconds(1), () -> 0L)
                .retry(1))
            .expectNext(1, 1)
            .verifyComplete();
        assertEquals(2, subscriptions.get());
    }

    @Test
    public void shouldCompensateDemandAndDiscardRejectedValues() {
        List<Integer> discarded = new ArrayList<>();
        Flux<Integer> distinct = DistinctDurationFlux
            .create(Flux.just(1, 1, 2), Function.identity(), Duration.ofSeconds(1), () -> 0L)
            .doOnDiscard(Integer.class, discarded::add);

        StepVerifier
            .create(distinct, 0)
            .thenRequest(2)
            .expectNext(1, 2)
            .verifyComplete();
        assertEquals(Arrays.asList(1), discarded);
    }

    @Test
    public void shouldPropagateSelectorFailureAndDiscardValue() {
        List<Integer> discarded = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("selector failure");

        StepVerifier
            .create(DistinctDurationFlux
                .create(
                    Flux.just(1),
                    ignored -> {
                        throw failure;
                    },
                    Duration.ofSeconds(1))
                .doOnDiscard(Integer.class, discarded::add))
            .expectErrorMatches(error -> error == failure)
            .verify();
        assertEquals(Arrays.asList(1), discarded);
    }

    @Test
    public void shouldProduceSameResultForFuseableAndNonFuseableSources() {
        List<Integer> fuseable = DistinctDurationFlux
            .create(Flux.just(1, 1, 2, 2), Function.identity(), Duration.ofSeconds(1), () -> 0L)
            .collectList()
            .block();
        List<Integer> nonFuseable = DistinctDurationFlux
            .create(Flux.just(1, 1, 2, 2).hide(), Function.identity(), Duration.ofSeconds(1), () -> 0L)
            .collectList()
            .block();

        assertEquals(Arrays.asList(1, 2), fuseable);
        assertEquals(fuseable, nonFuseable);
    }

    @Test
    public void shouldValidateAndPreserveDurationPrecision() {
        assertInvalidDuration(Duration.ZERO);
        assertInvalidDuration(Duration.ofNanos(-1));

        AtomicLong ticker = new AtomicLong();
        Flux<TimedValue> source = Flux
            .just(
                new TimedValue("first", "key", 0),
                new TimedValue("second", "key", 1))
            .doOnNext(value -> ticker.set(value.time));
        StepVerifier
            .create(DistinctDurationFlux.create(
                source,
                value -> value.key,
                Duration.ofNanos(1),
                ticker::get))
            .expectNextCount(2)
            .verifyComplete();

        StepVerifier
            .create(DistinctDurationFlux.create(
                Flux.just(1, 1),
                Function.identity(),
                Duration.ofSeconds(Long.MAX_VALUE),
                () -> 0L))
            .expectNext(1)
            .verifyComplete();
    }

    @Test
    public void shouldBoundStoreByActiveWindowAndClearIdempotently() {
        AtomicLong ticker = new AtomicLong();
        DistinctDurationFlux.DurationStore store = new DistinctDurationFlux.DurationStore();

        for (int window = 0; window < 10; window++) {
            ticker.set(window * 10L);
            for (int key = 0; key < 1_000; key++) {
                assertTrue(store.add(window * 1_000 + key, 10, ticker::get));
            }
            assertEquals(1_000, store.size());
        }

        store.clear();
        store.clear();
        assertEquals(0, store.size());
    }

    @Test
    public void shouldHandleHashCollisionsAndPartialLargeStateExpiry() {
        AtomicLong ticker = new AtomicLong();
        DistinctDurationFlux.DurationStore store = new DistinctDurationFlux.DurationStore();

        for (int key = 0; key < 20; key++) {
            ticker.set(key);
            assertTrue(store.add(new CollisionKey(key), 10, ticker::get));
        }
        ticker.set(20);
        assertFalse(store.add(new CollisionKey(15), 10, ticker::get));
        assertTrue(store.add(new CollisionKey(20), 10, ticker::get));
        assertEquals(10, store.size());

        ticker.set(40);
        assertTrue(store.add(new CollisionKey(40), 10, ticker::get));
        assertEquals(1, store.size());
    }

    @Test
    public void shouldKeepCollisionWindowExactAcrossResizes() {
        AtomicLong ticker = new AtomicLong();
        DistinctDurationFlux.DurationStore store = new DistinctDurationFlux.DurationStore();

        for (int key = 0; key < 1_000; key++) {
            ticker.set(key);
            CollisionKey value = new CollisionKey(key);
            assertTrue(store.add(value, 64, ticker::get));
            assertFalse(store.add(new CollisionKey(key), 64, ticker::get));
            assertEquals(Math.min(key + 1, 64), store.size());
        }

        ticker.set(1_000);
        assertFalse(store.add(new CollisionKey(999), 64, ticker::get));
        assertTrue(store.add(new CollisionKey(900), 64, ticker::get));
        assertEquals(64, store.size());
    }

    @Test
    public void shouldKeepSmallLargeBoundaryWindowBounded() {
        AtomicLong ticker = new AtomicLong();
        DistinctDurationFlux.DurationStore store = new DistinctDurationFlux.DurationStore();

        for (int key = 0; key < 1_000; key++) {
            ticker.set(key);
            assertTrue(store.add(new CollisionKey(key), 9, ticker::get));
            assertEquals(Math.min(key + 1, 9), store.size());
        }
    }

    @Test
    public void shouldPreserveBoundaryDuplicateSemantics() {
        AtomicLong ticker = new AtomicLong();
        DistinctDurationFlux.DurationStore store = new DistinctDurationFlux.DurationStore();

        for (int key = 0; key < 9; key++) {
            ticker.set(key);
            assertTrue(store.add(new CollisionKey(key), 9, ticker::get));
        }

        ticker.set(9);
        assertFalse(store.add(new CollisionKey(8), 9, ticker::get));
        assertEquals(8, store.size());
        assertTrue(store.add(new CollisionKey(9), 9, ticker::get));
        assertEquals(9, store.size());
    }

    @Test
    public void shouldMatchFixedWindowReferenceUnderCollisionChurn() {
        assertMatchesFixedWindowReference(CollisionKey::new);
    }

    @Test
    public void shouldMatchFixedWindowReferenceUnderSpreadChurn() {
        assertMatchesFixedWindowReference(value -> value);
    }

    private static void assertMatchesFixedWindowReference(Function<Integer, Object> keyFactory) {
        final long durationNanos = 17;
        AtomicLong ticker = new AtomicLong();
        DistinctDurationFlux.DurationStore store = new DistinctDurationFlux.DurationStore();
        Map<Integer, Long> expected = new LinkedHashMap<>();
        Random random = new Random(0x5EEDL);

        for (int operation = 0; operation < 20_000; operation++) {
            long now = ticker.addAndGet(random.nextInt(3));
            int key = random.nextInt(128);

            Iterator<Map.Entry<Integer, Long>> iterator = expected.entrySet().iterator();
            while (iterator.hasNext()) {
                if (now - iterator.next().getValue() >= durationNanos) {
                    iterator.remove();
                }
            }

            boolean expectedAdded = !expected.containsKey(key);
            if (expectedAdded) {
                expected.put(key, now);
            }

            assertEquals(
                "operation=" + operation + ", key=" + key + ", now=" + now,
                expectedAdded,
                store.add(keyFactory.apply(key), durationNanos, ticker::get));
            assertEquals(expected.size(), store.size());
        }
    }

    private static void assertInvalidDuration(Duration duration) {
        try {
            DistinctDurationFlux.create(Flux.just(1), Function.identity(), duration);
            fail("expected invalid duration: " + duration);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("duration"));
        }
    }

    private static final class TimedValue {
        private final String name;
        private final String key;
        private final long time;

        private TimedValue(String name, String key, long time) {
            this.name = name;
            this.key = key;
            this.time = time;
        }
    }

    private static final class CollisionKey {
        private final int value;

        private CollisionKey(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CollisionKey && ((CollisionKey) obj).value == value;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
