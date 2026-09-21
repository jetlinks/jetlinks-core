package org.jetlinks.core.config;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.Values;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class MonoConfigsReadTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Collection<String> KEYS = Arrays.asList("own", "parent");

    @Test
    public void scalarReadIsLazyFreshAndDoesNotResolveParentWhenComplete() {
        AtomicReference<String> current = new AtomicReference<>("before");
        AtomicInteger storageReads = new AtomicInteger();
        AtomicInteger configReads = new AtomicInteger();
        AtomicInteger parentReads = new AtomicInteger();
        StorageConfigurable owner = configurable(Mono.fromSupplier(() -> {
            storageReads.incrementAndGet();
            return storage(keys -> {
                configReads.incrementAndGet();
                return Mono.just(values("own", current.get(), "parent", "local"));
            });
        }), () -> {
            parentReads.incrementAndGet();
            return Mono.empty();
        });

        Mono<Values> query = owner.getConfigs(KEYS);
        assertEquals(0, storageReads.get());
        verifyValues(query, "own", "before", "parent", "local");
        current.set("after");
        verifyValues(query, "own", "after", "parent", "local");
        assertEquals(2, storageReads.get());
        assertEquals(2, configReads.get());
        assertEquals(0, parentReads.get());
        assertFalse(query instanceof Callable);
        assertFalse(query instanceof Fuseable);
    }

    @Test
    public void partialValuesOnlyReadMissingParentKeysAndKeepChildValues() {
        AtomicReference<Collection<String>> parentKeys = new AtomicReference<>();
        StorageConfigurable parent = configurable(Mono.just(storage(keys -> {
            parentKeys.set(keys);
            return Mono.just(values("parent", "parent-value"));
        })), Mono::empty);
        StorageConfigurable owner = configurable(
            Mono.just(storage(keys -> Mono.just(values("own", "child-value")))),
            () -> Mono.just(parent));

        StepVerifier.create(owner.getConfigs(KEYS))
                    .assertNext(values -> {
                        assertEquals("child-value", values.getValue("own").get().asString());
                        assertEquals("parent-value", values.getValue("parent").get().asString());
                    })
                    .verifyComplete();
        assertEquals(Collections.singleton("parent"), new HashSet<>(parentKeys.get()));
    }

    @Test
    public void disabledFallbackAndEmptyStoragePreserveOriginalBehavior() {
        AtomicInteger parentReads = new AtomicInteger();
        StorageConfigurable parent = configurable(
            Mono.just(storage(keys -> Mono.just(values("parent", "value")))), Mono::empty);
        Supplier<Mono<? extends Configurable>> parentSupplier = () -> {
            parentReads.incrementAndGet();
            return Mono.just(parent);
        };

        StorageConfigurable noFallback = configurable(
            Mono.just(storage(keys -> Mono.just(Values.of(Collections.emptyMap())))), parentSupplier);
        Mono<Values> query = noFallback.getConfigs(KEYS, false);
        assertTrue(query instanceof MonoConfigsRead);
        StepVerifier.create(query)
                    .assertNext(Values::isEmpty)
                    .verifyComplete();

        StorageConfigurable emptyStorage = configurable(Mono.empty(), parentSupplier);
        StepVerifier.create(emptyStorage.getConfigs(KEYS)).verifyComplete();
        assertEquals(0, parentReads.get());
    }

    @Test
    public void asynchronousSourcesKeepContextAndDoNotPrematurelyResolveParent() {
        Sinks.One<ConfigStorage> storageSource = Sinks.one();
        Sinks.One<Values> valuesSource = Sinks.one();
        AtomicInteger parentReads = new AtomicInteger();
        StorageConfigurable parent = configurable(Mono.deferContextual(context ->
                                                                           Mono.just(storage(keys -> Mono.just(values("parent", context.get("value")))))), Mono::empty);
        StorageConfigurable owner = configurable(
            storageSource.asMono(),
            () -> Mono.deferContextual(context -> {
                parentReads.incrementAndGet();
                assertEquals("context-value", context.get("value"));
                return Mono.just(parent);
            }));

        StepVerifier.create(owner
                                .getConfigs(KEYS)
                                .contextWrite(Context.of("value", "context-value")))
                    .then(() -> storageSource.emitValue(storage(keys -> valuesSource.asMono()), Sinks.EmitFailureHandler.FAIL_FAST))
                    .then(() -> assertEquals(0, parentReads.get()))
                    .then(() -> valuesSource.emitValue(values("own", "child"), Sinks.EmitFailureHandler.FAIL_FAST))
                    .assertNext(values -> {
                        assertEquals("child", values.getValue("own").get().asString());
                        assertEquals("context-value", values.getValue("parent").get().asString());
                    })
                    .verifyComplete();
        assertEquals(1, parentReads.get());
    }

    @Test
    public void demandCancellationAndErrorsArePreserved() {
        Values values = values("own", "value", "parent", "local");
        StorageConfigurable scalar = configurable(Mono.just(storage(keys -> Mono.just(values))), Mono::empty);
        StepVerifier.create(scalar.getConfigs(KEYS), 0)
                    .expectSubscription()
                    .thenRequest(1)
                    .expectNext(values)
                    .verifyComplete();

        AtomicInteger cancelled = new AtomicInteger();
        StorageConfigurable pending = configurable(
            Mono.just(storage(keys -> Mono.<Values>never().doOnCancel(cancelled::incrementAndGet))), Mono::empty);
        StepVerifier.create(pending.getConfigs(KEYS)).thenCancel().verify(TIMEOUT);
        assertEquals(1, cancelled.get());

        IllegalStateException failure = new IllegalStateException("failure");
        AtomicInteger parentReads = new AtomicInteger();
        StorageConfigurable failed = configurable(Mono.just(storage(keys -> Mono.error(failure))), () -> {
            parentReads.incrementAndGet();
            return Mono.empty();
        });
        StepVerifier.create(failed.getConfigs(KEYS))
                    .expectErrorMatches(error -> error == failure)
                    .verify(TIMEOUT);
        assertEquals(0, parentReads.get());
    }

    @Test
    public void repeatedConcurrentSubscriptionsHaveIndependentReads() {
        AtomicInteger reads = new AtomicInteger();
        StorageConfigurable owner = configurable(Mono.fromSupplier(() -> storage(keys ->
                                                                                     Mono.just(values("own", reads.incrementAndGet(), "parent", "local")))), Mono::empty);
        Mono<Values> query = owner.getConfigs(KEYS);

        StepVerifier.create(Flux.range(0, 512)
                                .flatMap(index -> query
                                    .subscribeOn(Schedulers.parallel())
                                    .map(values -> values.getValue("own").get().asInt()), 8)
                                .collectList())
                    .assertNext(values -> assertEquals(512, new HashSet<>(values).size()))
                    .verifyComplete();
        assertEquals(512, reads.get());
    }

    @Test
    public void assemblyAndErrorHooksArePreserved() {
        AtomicInteger assemblies = new AtomicInteger();
        AtomicReference<Object> errorData = new AtomicReference<>();
        IllegalStateException failure = new IllegalStateException("failure");
        IllegalArgumentException mapped = new IllegalArgumentException("mapped", failure);
        ConfigStorage storage = storage(keys -> {
            throw failure;
        });
        Hooks.onEachOperator("configs-read-test", publisher -> {
            if (MonoConfigsRead.class.isInstance(publisher)) {
                assemblies.incrementAndGet();
            }
            return publisher;
        });
        Hooks.onOperatorError("configs-read-test", (error, data) -> {
            assertSame(failure, error);
            errorData.set(data);
            return mapped;
        });
        try {
            StepVerifier.create(configurable(Mono.just(storage), Mono::empty).getConfigs(KEYS))
                        .expectErrorMatches(error -> error == mapped)
                        .verify(TIMEOUT);
            assertEquals(1, assemblies.get());
            assertSame(storage, errorData.get());
        } finally {
            Hooks.resetOnEachOperator("configs-read-test");
            Hooks.resetOnOperatorError("configs-read-test");
        }
    }

    @Test
    public void typedDuplicateKeysRemainDeduplicated() {
        AtomicInteger reads = new AtomicInteger();
        StorageConfigurable owner = configurable(Mono.just(storage(keys -> {
            assertEquals(Collections.singleton("own"), new HashSet<>(keys));
            reads.incrementAndGet();
            return Mono.just(values("own", "value"));
        })), Mono::empty);
        ConfigKey<String> key = ConfigKey.of("own", "own", String.class);

        StepVerifier.create(owner.getConfigs(key, key))
                    .assertNext(values -> assertEquals("value", values.getString("own", (String) null)))
                    .verifyComplete();
        StepVerifier.create(owner.getConfigs(key, key, key))
                    .assertNext(values -> assertEquals("value", values.getString("own", (String) null)))
                    .verifyComplete();
        assertEquals(2, reads.get());
    }

    private static ConfigStorage storage(Function<Collection<String>, Mono<Values>> reader) {
        return new InMemoryConfigStorage() {
            @Override
            public Mono<Values> getConfigs(Collection<String> keys) {
                return reader.apply(keys);
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

    private static Values values(String key1, Object value1, String key2, Object value2) {
        return Values.of(Map.of(key1, value1, key2, value2));
    }

    private static Values values(String key, Object value) {
        return Values.of(Collections.singletonMap(key, value));
    }

    private static void verifyValues(Mono<Values> query,
                                     String key1,
                                     Object value1,
                                     String key2,
                                     Object value2) {
        StepVerifier.create(query.map(Values::getAllValues))
                    .expectNext(Map.of(key1, value1, key2, value2))
                    .verifyComplete();
    }
}
