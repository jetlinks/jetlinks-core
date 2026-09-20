package org.jetlinks.core.defaults;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MonoVersionedMetadataTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    public void cacheHitAvoidsLoader() {
        AtomicInteger loads = new AtomicInteger();
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just(1L),
            () -> "cached",
            (version, cached) -> version == 1L,
            version -> Mono.fromSupplier(() -> "loaded-" + loads.incrementAndGet()),
            () -> Mono.fromSupplier(() -> "empty-" + loads.incrementAndGet()));

        StepVerifier.create(metadata).expectNext("cached").verifyComplete();
        assertEquals(0, loads.get());
    }

    @Test
    public void cachedValueCancellationSkipsOnComplete() {
        AtomicInteger completions = new AtomicInteger();
        AtomicReference<String> value = new AtomicReference<>();
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just(1L),
            () -> "cached",
            (version, cached) -> version == 1L,
            version -> Mono.just("loaded"),
            Mono::empty);

        metadata.subscribe(new BaseSubscriber<String>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(1);
            }

            @Override
            protected void hookOnNext(String cached) {
                value.set(cached);
                cancel();
            }

            @Override
            protected void hookOnComplete() {
                completions.incrementAndGet();
            }
        });

        assertEquals("cached", value.get());
        assertEquals(0, completions.get());
    }

    @Test
    public void versionMismatchUsesAsyncLoaderWithoutPrematureCompletion() {
        Sinks.One<String> loader = Sinks.one();
        AtomicInteger loads = new AtomicInteger();
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just(2L),
            () -> "cached",
            (version, cached) -> version == 1L,
            version -> {
                loads.incrementAndGet();
                return loader.asMono();
            },
            Mono::empty);

        StepVerifier.create(metadata)
                    .then(() -> assertEquals(1, loads.get()))
                    .then(() -> loader.emitValue("loaded", Sinks.EmitFailureHandler.FAIL_FAST))
                    .expectNext("loaded")
                    .verifyComplete();
    }

    @Test
    public void emptyVersionUsesEmptyLoader() {
        AtomicInteger emptyLoads = new AtomicInteger();
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.empty(),
            () -> "cached",
            (version, cached) -> true,
            version -> Mono.just("version"),
            () -> Mono.fromSupplier(() -> "empty-" + emptyLoads.incrementAndGet()));

        StepVerifier.create(metadata).expectNext("empty-1").verifyComplete();
        assertEquals(1, emptyLoads.get());
    }

    @Test
    public void rawVersionIsConvertedInsideOperator() {
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just("2"),
            new MonoVersionedMetadata.Loader<Long, String>() {
                @Override
                public Long convertVersion(Object value) {
                    return Long.parseLong((String) value);
                }

                @Override
                public String getCached() {
                    return "cached";
                }

                @Override
                public boolean isValid(Long version, String cached) {
                    return version == 1L;
                }

                @Override
                public Mono<String> load(Long version) {
                    return Mono.just("loaded-" + version);
                }
            }
        );

        StepVerifier.create(metadata).expectNext("loaded-2").verifyComplete();
    }

    @Test
    public void nullConvertedVersionUsesEmptyLoader() {
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just("empty"),
            new MonoVersionedMetadata.Loader<Long, String>() {
                @Override
                public Long convertVersion(Object value) {
                    return null;
                }

                @Override
                public String getCached() {
                    return "cached";
                }

                @Override
                public boolean isValid(Long version, String cached) {
                    return true;
                }

                @Override
                public Mono<String> load(Long version) {
                    return Mono.just("version");
                }

                @Override
                public Mono<String> loadEmpty() {
                    return Mono.just("empty");
                }
            }
        );

        StepVerifier.create(metadata).expectNext("empty").verifyComplete();
    }

    @Test
    public void cacheValidationDoesNotMixSnapshots() {
        AtomicReference<String> cached = new AtomicReference<>("old");
        AtomicLong cachedVersion = new AtomicLong(1L);
        CountDownLatch cacheRead = new CountDownLatch(1);
        CountDownLatch cacheUpdated = new CountDownLatch(1);
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just(2L),
            () -> {
                String snapshot = cached.get();
                cacheRead.countDown();
                await(cacheUpdated);
                return snapshot;
            },
            (version, snapshot) -> version == cachedVersion.get(),
            version -> Mono.just("loaded-" + version),
            Mono::empty
        );

        StepVerifier.create(metadata.subscribeOn(Schedulers.boundedElastic()))
                    .then(() -> {
                        await(cacheRead);
                        cached.set("new");
                        cachedVersion.set(2L);
                        cacheUpdated.countDown();
                    })
                    .expectNext("loaded-2")
                    .verifyComplete();
    }

    @Test
    public void normalVersionTransitionCompletesVersionSource() {
        AtomicReference<SignalType> signal = new AtomicReference<>();
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just(2L).hide().doFinally(signal::set),
            () -> null,
            (version, cached) -> false,
            version -> Mono.just("loaded"),
            Mono::empty
        );

        StepVerifier.create(metadata)
                    .expectNext("loaded")
                    .verifyComplete();
        assertEquals(SignalType.ON_COMPLETE, signal.get());
    }

    @Test
    public void demandAndCancellationReachActiveSource() {
        AtomicInteger versionSubscriptions = new AtomicInteger();
        AtomicInteger loaderSubscriptions = new AtomicInteger();
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.defer(() -> {
                versionSubscriptions.incrementAndGet();
                return Mono.just(2L);
            }),
            () -> null,
            (version, cached) -> false,
            version -> Mono.defer(() -> {
                loaderSubscriptions.incrementAndGet();
                return Mono.just("loaded");
            }),
            Mono::empty);

        StepVerifier.create(metadata, 0)
                    .expectSubscription()
                    .then(() -> assertEquals(1, versionSubscriptions.get()))
                    .then(() -> assertEquals(0, loaderSubscriptions.get()))
                    .thenRequest(1)
                    .expectNext("loaded")
                    .verifyComplete();

        AtomicInteger cancelled = new AtomicInteger();
        AtomicInteger pendingSubscriptions = new AtomicInteger();
        Mono<String> pending = MonoVersionedMetadata.create(
            Mono.just(2L),
            () -> null,
            (version, cached) -> false,
            version -> Mono.<String>never()
                           .doOnSubscribe(ignore -> pendingSubscriptions.incrementAndGet())
                           .doOnCancel(cancelled::incrementAndGet),
            Mono::empty);
        StepVerifier.create(pending)
                    .then(() -> assertEquals(1, pendingSubscriptions.get()))
                    .thenCancel()
                    .verify(TIMEOUT);
        assertEquals(1, cancelled.get());
    }

    @Test
    public void contextReachesVersionAndLoader() {
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.deferContextual(context -> Mono.just(context.get("version"))),
            () -> null,
            (version, cached) -> false,
            version -> Mono.deferContextual(context -> Mono.just(version + "-" + context.get("value"))),
            Mono::empty);

        StepVerifier.create(metadata.contextWrite(Context.of("version", "v1", "value", "metadata")))
                    .expectNext("v1-metadata")
                    .verifyComplete();
    }

    @Test
    public void errorsDoNotSwitchToOtherSources() {
        IllegalStateException versionFailure = new IllegalStateException("version");
        AtomicInteger loads = new AtomicInteger();
        Mono<String> failedVersion = MonoVersionedMetadata.create(
            Mono.error(versionFailure),
            () -> null,
            (version, cached) -> false,
            version -> Mono.fromSupplier(() -> "loaded-" + loads.incrementAndGet()),
            Mono::empty);
        StepVerifier.create(failedVersion)
                    .expectErrorMatches(error -> error == versionFailure)
                    .verify(TIMEOUT);
        assertEquals(0, loads.get());

        IllegalArgumentException loaderFailure = new IllegalArgumentException("loader");
        Mono<String> failedLoader = MonoVersionedMetadata.create(
            Mono.just(2L),
            () -> null,
            (version, cached) -> false,
            version -> Mono.error(loaderFailure),
            Mono::empty);
        StepVerifier.create(failedLoader)
                    .expectErrorMatches(error -> error == loaderFailure)
                    .verify(TIMEOUT);
    }

    @Test
    public void supplierValidatorAndLoaderFailuresArePropagated() {
        assertFailure(() -> {
            throw new IllegalStateException("cached");
        }, (version, cached) -> false, version -> Mono.just("loaded"), "cached");
        assertFailure(() -> "cached", (version, cached) -> {
            throw new IllegalStateException("validator");
        }, version -> Mono.just("loaded"), "validator");
        assertFailure(() -> null, (version, cached) -> false, version -> {
            throw new IllegalStateException("loader");
        }, "loader");

        Mono<String> emptyFailure = MonoVersionedMetadata.create(
            Mono.empty(),
            () -> null,
            (version, cached) -> false,
            version -> Mono.just("loaded"),
            () -> {
                throw new IllegalStateException("empty");
            });
        StepVerifier.create(emptyFailure)
                    .expectErrorMessage("empty")
                    .verify(TIMEOUT);
    }

    @Test
    public void repeatedConcurrentSubscriptionsKeepIndependentState() {
        AtomicInteger loads = new AtomicInteger();
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just(2L),
            () -> null,
            (version, cached) -> false,
            version -> Mono.fromSupplier(() -> String.valueOf(loads.incrementAndGet())),
            Mono::empty);

        StepVerifier.create(Flux.range(0, 512)
                                .flatMap(index -> metadata.subscribeOn(Schedulers.parallel()), 8)
                                .collectList())
                    .assertNext(values -> assertEquals(512, new HashSet<>(values).size()))
                    .verifyComplete();
        assertEquals(512, loads.get());
    }

    private static void assertFailure(java.util.function.Supplier<String> cachedSupplier,
                                      java.util.function.BiPredicate<Long, String> validator,
                                      java.util.function.Function<Long, Mono<String>> loader,
                                      String message) {
        Mono<String> metadata = MonoVersionedMetadata.create(
            Mono.just(2L), cachedSupplier, validator, loader, Mono::empty);
        StepVerifier.create(metadata)
                    .expectErrorMessage(message)
                    .verify(TIMEOUT);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }
}
