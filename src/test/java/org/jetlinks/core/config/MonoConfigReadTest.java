package org.jetlinks.core.config;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.Value;
import org.junit.Test;
import reactor.core.Fuseable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class MonoConfigReadTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    public void scalarReadIsLazyFreshAndDoesNotResolveParent() {
        AtomicReference<String> current = new AtomicReference<>("before");
        AtomicInteger storageReads = new AtomicInteger();
        AtomicInteger configReads = new AtomicInteger();
        AtomicInteger parentReads = new AtomicInteger();
        ConfigStorage storage = storage(key -> {
            configReads.incrementAndGet();
            return Mono.just(Value.simple(current.get()));
        });
        StorageConfigurable owner = configurable(Mono.fromSupplier(() -> {
            storageReads.incrementAndGet();
            return storage;
        }), () -> {
            parentReads.incrementAndGet();
            return Mono.empty();
        });

        Mono<Value> query = owner.getConfig("key");
        assertEquals(0, storageReads.get());
        assertEquals(0, configReads.get());
        verifyValue(query, "before");
        current.set("after");
        verifyValue(query, "after");
        assertEquals(2, storageReads.get());
        assertEquals(2, configReads.get());
        assertEquals(0, parentReads.get());
        assertFalse(query instanceof Callable);
        assertFalse(query instanceof Fuseable);
    }

    @Test
    public void emptyConfigFallsBackOnlyWhenRequested() {
        AtomicInteger parentReads = new AtomicInteger();
        StorageConfigurable parent = configurable(Mono.just(storage(key -> Mono.just(Value.simple("parent")))), Mono::empty);
        StorageConfigurable owner = configurable(Mono.just(storage(key -> Mono.empty())), () -> {
            parentReads.incrementAndGet();
            return Mono.just(parent);
        });

        verifyValue(owner.getConfig("key"), "parent");
        StepVerifier.create(owner.getConfig("key", false)).expectComplete().verify(TIMEOUT);
        assertEquals(1, parentReads.get());
    }

    @Test
    public void typedScalarReadConvertsInsideConfigOperator() {
        ConfigKey<Integer> key = ConfigKey.of("key", "key", Integer.class);
        AtomicReference<String> current = new AtomicReference<>("1");
        StorageConfigurable owner = configurable(
            Mono.just(storage(ignore -> Mono.just(Value.simple(current.get())))),
            Mono::empty
        );

        Mono<Integer> query = owner.getConfig(key, false);
        assertTrue(query instanceof MonoConfigRead);
        StepVerifier.create(query).expectNext(1).verifyComplete();
        current.set("2");
        StepVerifier.create(query).expectNext(2).verifyComplete();
    }

    @Test
    public void typedScalarReadConvertsInsideParentOperator() {
        ConfigKey<Integer> key = ConfigKey.of("key", "key", Integer.class);
        StorageConfigurable parent = configurable(
            Mono.just(storage(ignore -> Mono.just(Value.simple("3")))),
            Mono::empty
        );
        StorageConfigurable owner = configurable(
            Mono.just(storage(ignore -> Mono.empty())),
            () -> Mono.just(parent)
        );

        StepVerifier.create(owner.getConfig(key)).expectNext(3).verifyComplete();
    }

    @Test
    public void emptyStoragePreservesParentFallback() {
        StorageConfigurable parent = configurable(Mono.just(storage(key -> Mono.just(Value.simple("parent")))), Mono::empty);
        StorageConfigurable owner = configurable(Mono.empty(), () -> Mono.just(parent));
        verifyValue(owner.getConfig("key"), "parent");
        StepVerifier.create(owner.getConfig("key", false)).expectComplete().verify(TIMEOUT);

        StorageConfigurable hidden = configurable(Mono.<ConfigStorage>empty().hide(), () -> Mono.just(parent));
        verifyValue(hidden.getConfig("key"), "parent");
    }

    @Test
    public void asynchronousStorageIsSubscribedOnce() {
        Sinks.One<ConfigStorage> storageSource = Sinks.one();
        AtomicInteger subscriptions = new AtomicInteger();
        AtomicInteger reads = new AtomicInteger();
        ConfigStorage storage = storage(key -> {
            reads.incrementAndGet();
            return Mono.just(Value.simple("value"));
        });
        StorageConfigurable owner = configurable(storageSource
                                                     .asMono()
                                                     .doOnSubscribe(ignored -> subscriptions.incrementAndGet()), Mono::empty);

        StepVerifier.create(owner.getConfig("key").map(Value::asString))
                    .then(() -> assertEquals(Sinks.EmitResult.OK, storageSource.tryEmitValue(storage)))
                    .expectNext("value")
                    .expectComplete()
                    .verify(TIMEOUT);
        assertEquals(1, subscriptions.get());
        assertEquals(1, reads.get());
    }

    @Test
    public void asynchronousConfigIsNotReadAgainOrTreatedAsEmpty() {
        Sinks.One<Value> source = Sinks.one();
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger parentReads = new AtomicInteger();
        AtomicInteger subscriptions = new AtomicInteger();
        StorageConfigurable owner = configurable(Mono.just(storage(key -> {
            reads.incrementAndGet();
            return source.asMono();
        })), () -> {
            parentReads.incrementAndGet();
            return Mono.empty();
        });

        StepVerifier
            .create(owner
                        .getConfig("key")
                        .doOnSubscribe(ignored -> subscriptions.incrementAndGet())
                        .map(Value::asString))
            .then(() -> assertEquals(0, parentReads.get()))
            .then(() -> assertEquals(Sinks.EmitResult.OK, source.tryEmitValue(Value.simple("value"))))
            .expectNext("value")
            .expectComplete()
            .verify(TIMEOUT);
        assertEquals(1, reads.get());
        assertEquals(1, subscriptions.get());
        assertEquals(0, parentReads.get());
    }

    @Test
    public void asynchronousEmptyConfigResolvesParentWithContext() {
        Sinks.One<Value> source = Sinks.one();
        AtomicInteger parentReads = new AtomicInteger();
        StorageConfigurable parent = configurable(Mono.deferContextual(context ->
                                                                           Mono.just(storage(key -> Mono.deferContextual(inner -> Mono.just(Value.simple(inner.get("value"))))))), Mono::empty);
        StorageConfigurable owner = configurable(Mono.just(storage(key -> source.asMono())), () -> {
            parentReads.incrementAndGet();
            return Mono.deferContextual(context -> {
                assertEquals("context-value", context.get("value"));
                return Mono.just(parent);
            });
        });

        StepVerifier
            .create(owner.getConfig("key").map(Value::asString).contextWrite(Context.of("value", "context-value")))
            .then(() -> assertEquals(Sinks.EmitResult.OK, source.tryEmitEmpty()))
            .expectNext("context-value")
            .expectComplete()
            .verify(TIMEOUT);
        assertEquals(1, parentReads.get());
    }

    @Test
    public void contextReachesStorageAndConfigSources() {
        Mono<ConfigStorage> source = Mono.deferContextual(context -> {
            assertEquals("context-value", context.get("value"));
            return Mono.just(storage(key -> Mono.deferContextual(inner -> Mono.just(Value.simple(inner.get("value"))))));
        });
        StorageConfigurable owner = configurable(source, Mono::empty);
        verifyValue(owner.getConfig("key").contextWrite(Context.of("value", "context-value")), "context-value");
    }

    @Test
    public void scalarReadHonorsDemandAndCancellationDiscard() {
        Value value = Value.simple("value");
        StorageConfigurable owner = configurable(Mono.just(storage(key -> Mono.just(value))), Mono::empty);
        StepVerifier.create(owner.getConfig("key"), 0)
                    .expectSubscription()
                    .thenRequest(1)
                    .expectNext(value)
                    .expectComplete()
                    .verify(TIMEOUT);

        AtomicReference<Value> discarded = new AtomicReference<>();
        StepVerifier.create(owner.getConfig("key").doOnDiscard(Value.class, discarded::set), 0)
                    .thenCancel()
                    .verify(TIMEOUT);
        assertSame(value, discarded.get());
    }

    @Test
    public void cancellationReachesStorageConfigAndParent() {
        AtomicInteger cancelled = new AtomicInteger();
        StorageConfigurable storagePending = configurable(
            Mono.<ConfigStorage>never().doOnCancel(cancelled::incrementAndGet), Mono::empty);
        StepVerifier.create(storagePending.getConfig("key")).thenCancel().verify(TIMEOUT);

        StorageConfigurable configPending = configurable(Mono.just(storage(key ->
                                                                               Mono
                                                                                   .<Value>never()
                                                                                   .doOnCancel(cancelled::incrementAndGet))), Mono::empty);
        StepVerifier.create(configPending.getConfig("key")).thenCancel().verify(TIMEOUT);

        StorageConfigurable parentPending = configurable(Mono.just(storage(key -> Mono.empty())), () ->
            Mono.<Configurable>never().doOnCancel(cancelled::incrementAndGet));
        StepVerifier.create(parentPending.getConfig("key")).thenCancel().verify(TIMEOUT);
        assertEquals(3, cancelled.get());
    }

    @Test
    public void failuresDoNotFallBackToParent() {
        IllegalStateException failure = new IllegalStateException("failure");
        AtomicInteger parentReads = new AtomicInteger();
        Supplier<Mono<? extends Configurable>> parent = () -> {
            parentReads.incrementAndGet();
            return Mono.empty();
        };
        verifyError(configurable(Mono.fromCallable(() -> {
            throw failure;
        }), parent).getConfig("key"), failure);
        verifyError(configurable(Mono.<ConfigStorage>error(failure).hide(), parent).getConfig("key"), failure);
        verifyError(configurable(Mono.just(storage(key -> {
            throw failure;
        })), parent).getConfig("key"), failure);
        verifyError(configurable(Mono.just(storage(key -> Mono.error(failure))), parent).getConfig("key"), failure);
        verifyError(configurable(Mono.just(storage(key -> Mono
            .<Value>error(failure)
            .hide())), parent).getConfig("key"), failure);
        assertEquals(0, parentReads.get());
    }

    @Test
    public void invalidPublishersAndParentFailuresRemainErrors() {
        StorageConfigurable invalid = configurable(Mono.just(storage(key -> null)), Mono::empty);
        StepVerifier.create(invalid.getConfig("key"))
                    .expectError(NullPointerException.class)
                    .verify(TIMEOUT);
        IllegalStateException failure = new IllegalStateException("parent");
        StorageConfigurable parentFailure = configurable(Mono.just(storage(key -> Mono.empty())), () -> {
            throw failure;
        });
        verifyError(parentFailure.getConfig("key"), failure);
    }

    @Test
    public void repeatedConcurrentSubscriptionsHaveIndependentReads() {
        AtomicInteger reads = new AtomicInteger();
        StorageConfigurable owner = configurable(Mono.fromSupplier(() -> storage(key ->
                                                                                     Mono.just(Value.simple(reads.incrementAndGet())))), Mono::empty);
        Mono<Value> query = owner.getConfig("key");

        StepVerifier.create(Flux.range(0, 512)
                                .flatMap(index -> query.subscribeOn(Schedulers.parallel()).map(Value::asInt), 8)
                                .collectList())
                    .assertNext(values -> assertEquals(512, new HashSet<>(values).size()))
                    .expectComplete()
                    .verify(TIMEOUT);
        assertEquals(512, reads.get());
    }

    @Test
    public void assemblyAndErrorHooksArePreserved() {
        AtomicInteger assemblies = new AtomicInteger();
        AtomicReference<Object> errorData = new AtomicReference<>();
        IllegalStateException failure = new IllegalStateException("failure");
        IllegalArgumentException mapped = new IllegalArgumentException("mapped", failure);
        ConfigStorage storage = storage(key -> {
            throw failure;
        });
        Hooks.onEachOperator("config-read-test", publisher -> {
            if (MonoConfigRead.class.isInstance(publisher)) {
                assemblies.incrementAndGet();
            }
            return publisher;
        });
        Hooks.onOperatorError("config-read-test", (error, data) -> {
            assertSame(failure, error);
            errorData.set(data);
            return mapped;
        });
        try {
            verifyError(configurable(Mono.just(storage), Mono::empty).getConfig("key"), mapped);
            assertEquals(1, assemblies.get());
            assertSame(storage, errorData.get());
        } finally {
            Hooks.resetOnEachOperator("config-read-test");
            Hooks.resetOnOperatorError("config-read-test");
        }
    }

    @Test
    public void debugHooksHiddenSourcesAndDownstreamOperatorsWork() {
        Hooks.onOperatorDebug();
        try {
            StorageConfigurable owner = configurable(Mono
                                                         .just(storage(key -> Mono.just(Value.simple("value")).hide()))
                                                         .hide(), Mono::empty);
            StepVerifier.create(owner.getConfig("key")
                                     .map(Value::asString)
                                     .filter("value"::equals)
                                     .hide()
                                     .flatMap(Mono::just)
                                     .publishOn(Schedulers.parallel()))
                        .expectNext("value")
                        .expectComplete()
                        .verify(TIMEOUT);
        } finally {
            Hooks.resetOnOperatorDebug();
        }
    }

    private static ConfigStorage storage(Function<String, Mono<Value>> reader) {
        return new InMemoryConfigStorage() {
            @Override
            public Mono<Value> getConfig(String key) {
                return reader.apply(key);
            }
        };
    }

    private static StorageConfigurable configurable(Mono<ConfigStorage> source,
                                                    Supplier<Mono<? extends Configurable>> parent) {
        return new StorageConfigurable() {
            @Override
            public Mono<ConfigStorage> getReactiveStorage() {
                return source;
            }

            @Override
            public Mono<? extends Configurable> getParent() {
                return parent.get();
            }
        };
    }

    private static void verifyValue(Mono<Value> query, String value) {
        StepVerifier.create(query.map(Value::asString)).expectNext(value).expectComplete().verify(TIMEOUT);
    }

    private static void verifyError(Mono<Value> query, Throwable expected) {
        StepVerifier.create(query).expectErrorMatches(error -> error == expected).verify(TIMEOUT);
    }
}
