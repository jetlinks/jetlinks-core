package org.jetlinks.core.defaults;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.InMemoryConfigStorage;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.junit.Test;
import reactor.core.Fuseable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class MonoProtocolSupportTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    public void scalarReadIsLazyFreshAndDoesNotCacheProtocol() {
        AtomicReference<String> current = new AtomicReference<>("first");
        AtomicInteger storageReads = new AtomicInteger();
        AtomicInteger configReads = new AtomicInteger();
        AtomicInteger protocolReads = new AtomicInteger();
        ProtocolSupport first = protocol("first");
        ProtocolSupport second = protocol("second");
        Mono<ProtocolSupport> source = MonoProtocolSupport.create(
            Mono.fromSupplier(() -> {
                storageReads.incrementAndGet();
                return storage(() -> {
                    configReads.incrementAndGet();
                    return Mono.just(Value.simple(current.get()));
                });
            }),
            supports(id -> {
                protocolReads.incrementAndGet();
                return Mono.just("first".equals(id) ? first : second);
            }),
            "protocol",
            null
        );

        assertEquals(0, storageReads.get());
        assertSame(first, source.block(TIMEOUT));
        current.set("second");
        assertSame(second, source.block(TIMEOUT));
        assertEquals(2, storageReads.get());
        assertEquals(2, configReads.get());
        assertEquals(2, protocolReads.get());
        assertFalse(source instanceof Callable);
        assertFalse(source instanceof Fuseable);
    }

    @Test
    public void emptyConfigAndEmptyProtocolFallBackToProduct() {
        ProtocolSupport expected = protocol("parent");
        AtomicInteger productReads = new AtomicInteger();
        Mono<DeviceProductOperator> product = Mono.fromSupplier(() -> {
            productReads.incrementAndGet();
            return product(Mono.just(expected));
        });

        StepVerifier.create(MonoProtocolSupport.create(
                        Mono.just(storage(Mono::empty)),
                        supports(id -> Mono.empty()),
                        "protocol",
                        product))
                    .expectNext(expected)
                    .verifyComplete();

        StepVerifier.create(MonoProtocolSupport.create(
                        Mono.just(storage(() -> Mono.just(Value.simple("missing")))),
                        supports(id -> Mono.empty()),
                        "protocol",
                        product))
                    .expectNext(expected)
                    .verifyComplete();
        assertEquals(2, productReads.get());
    }

    @Test
    public void internalProductLookupRemainsLazyAndReadsCurrentVersion() {
        List<String> keys = List.of("productId", "productVersion");
        AtomicInteger configReads = new AtomicInteger();
        AtomicInteger productReads = new AtomicInteger();
        ProtocolSupport expected = protocol("parent");
        ConfigStorage productStorage = productStorage(requested -> {
            assertEquals(keys, requested);
            configReads.incrementAndGet();
            return Mono.just(Values.of(Map.of("productId", "prod", "productVersion", "v1")));
        });
        DeviceRegistry registry = registry((id, version) -> {
            assertEquals("prod", id);
            assertEquals("v1", version);
            productReads.incrementAndGet();
            return Mono.just(product(Mono.just(expected)));
        });

        Mono<ProtocolSupport> source = MonoProtocolSupport.create(
            Mono.just(storage(Mono::empty)),
            supports(id -> Mono.empty()),
            "protocol",
            MonoDeviceProduct.create(Mono.just(productStorage), registry, keys, "productId", "productVersion")
        );
        assertEquals(0, configReads.get());
        StepVerifier.create(source).expectNext(expected).verifyComplete();
        StepVerifier.create(source).expectNext(expected).verifyComplete();
        assertEquals(2, configReads.get());
        assertEquals(2, productReads.get());
    }

    @Test
    public void internalProductLookupCompletesWhenProductIsMissing() {
        List<String> keys = List.of("productId", "productVersion");
        AtomicReference<Values> values = new AtomicReference<>(Values.of(Map.of("productVersion", "v1")));
        AtomicInteger productReads = new AtomicInteger();
        ConfigStorage productStorage = productStorage(requested -> Mono.just(values.get()));
        DeviceRegistry registry = registry((id, version) -> {
            productReads.incrementAndGet();
            return Mono.empty();
        });
        Mono<ProtocolSupport> source = MonoProtocolSupport.create(
            Mono.just(storage(Mono::empty)),
            supports(id -> Mono.empty()),
            "protocol",
            MonoDeviceProduct.create(Mono.just(productStorage), registry, keys, "productId", "productVersion")
        );
        StepVerifier.create(source).verifyComplete();
        assertEquals(0, productReads.get());
        values.set(Values.of(Map.of("productId", "prod")));
        StepVerifier.create(source).verifyComplete();
        assertEquals(1, productReads.get());
    }

    @Test
    public void internalProductLookupPreservesAsyncContextAndCancellation() {
        List<String> keys = List.of("productId", "productVersion");
        ProtocolSupport expected = protocol("parent");
        AtomicReference<Mono<Values>> valuesSource = new AtomicReference<>(Mono.deferContextual(context -> {
            assertEquals("context", context.get("value"));
            return Mono.just(Values.of(Map.of("productId", "prod")));
        }));
        ConfigStorage productStorage = productStorage(requested -> {
            assertEquals(keys, requested);
            return valuesSource.get();
        });
        DeviceRegistry registry = registry((id, version) -> Mono.deferContextual(context -> {
            assertEquals("prod", id);
            assertNull(version);
            assertEquals("context", context.get("value"));
            return Mono.just(product(Mono.just(expected)));
        }));
        Mono<ProtocolSupport> source = MonoProtocolSupport.create(
            Mono.just(storage(Mono::empty)),
            supports(id -> Mono.empty()),
            "protocol",
            MonoDeviceProduct.create(Mono.just(productStorage), registry, keys, "productId", "productVersion")
        );
        StepVerifier.create(source.contextWrite(Context.of("value", "context")))
                    .expectNext(expected)
                    .verifyComplete();

        AtomicInteger cancelled = new AtomicInteger();
        Sinks.One<Values> pending = Sinks.one();
        valuesSource.set(pending.asMono().doOnCancel(cancelled::incrementAndGet));
        StepVerifier.create(source, 0)
                    .thenRequest(1)
                    .thenCancel()
                    .verify(TIMEOUT);
        assertEquals(1, cancelled.get());
    }

    @Test
    public void internalProductLookupPropagatesErrorsWithoutAnotherFallback() {
        List<String> keys = List.of("productId", "productVersion");
        IllegalStateException failure = new IllegalStateException("product lookup");
        ConfigStorage productStorage = productStorage(requested -> Mono.just(Values.of(Map.of("productId", "prod"))));
        DeviceRegistry registry = registry((id, version) -> Mono.error(failure));

        verifyError(MonoProtocolSupport.create(
            Mono.just(storage(Mono::empty)),
            supports(id -> Mono.empty()),
            "protocol",
            MonoDeviceProduct.create(Mono.just(productStorage), registry, keys, "productId", "productVersion")
        ), failure);

        ConfigStorage failingStorage = productStorage(requested -> {
            throw failure;
        });
        verifyError(MonoProtocolSupport.create(
            Mono.just(storage(Mono::empty)),
            supports(id -> Mono.empty()),
            "protocol",
            MonoDeviceProduct.create(Mono.just(failingStorage), registry, keys, "productId", "productVersion")
        ), failure);
    }

    @Test
    public void asynchronousStagesPreserveContext() {
        ProtocolSupport expected = protocol("async");
        Mono<ConfigStorage> source = Mono.deferContextual(context -> {
            assertEquals("context", context.get("value"));
            return Mono.just(storage(() -> Mono.deferContextual(inner -> {
                assertEquals("context", inner.get("value"));
                return Mono.just(Value.simple("async"));
            })));
        });
        ProtocolSupports supports = supports(id -> Mono.deferContextual(context -> {
            assertEquals("context", context.get("value"));
            return Mono.just(expected);
        }));

        StepVerifier.create(MonoProtocolSupport
                                .create(source, supports, "protocol", null)
                                .contextWrite(Context.of("value", "context")))
                    .expectNext(expected)
                    .verifyComplete();
    }

    @Test
    public void demandAndCancellationReachActiveSource() {
        AtomicInteger storageReads = new AtomicInteger();
        ProtocolSupport expected = protocol("test");
        Mono<ProtocolSupport> source = MonoProtocolSupport.create(
            Mono.fromSupplier(() -> {
                storageReads.incrementAndGet();
                return storage(() -> Mono.just(Value.simple("test")));
            }),
            supports(id -> Mono.just(expected)),
            "protocol",
            null
        );
        StepVerifier.create(source, 0)
                    .expectSubscription()
                    .then(() -> assertEquals(0, storageReads.get()))
                    .thenRequest(1)
                    .expectNext(expected)
                    .verifyComplete();
        StepVerifier.create(source, 0).thenCancel().verify(TIMEOUT);
        assertEquals(1, storageReads.get());

        AtomicInteger cancelled = new AtomicInteger();
        Sinks.One<Value> pending = Sinks.one();
        Mono<ProtocolSupport> cancellable = MonoProtocolSupport.create(
            Mono.just(storage(() -> pending.asMono().doOnCancel(cancelled::incrementAndGet))),
            supports(id -> Mono.just(expected)),
            "protocol",
            null
        );
        StepVerifier.create(cancellable, 0)
                    .thenRequest(1)
                    .thenCancel()
                    .verify(TIMEOUT);
        assertEquals(1, cancelled.get());
    }

    @Test
    public void failuresDoNotFallBackToProduct() {
        IllegalStateException failure = new IllegalStateException("failure");
        AtomicInteger productReads = new AtomicInteger();
        Mono<DeviceProductOperator> product = Mono.fromSupplier(() -> {
            productReads.incrementAndGet();
            return product(Mono.empty());
        });

        verifyError(MonoProtocolSupport.create(
            Mono.fromCallable(() -> {
                throw failure;
            }), supports(id -> Mono.empty()), "protocol", product), failure);
        verifyError(MonoProtocolSupport.create(
            Mono.just(storage(() -> Mono.error(failure))),
            supports(id -> Mono.empty()), "protocol", product), failure);
        verifyError(MonoProtocolSupport.create(
            Mono.just(storage(() -> Mono.just(Value.simple("test")))),
            supports(id -> Mono.error(failure)), "protocol", product), failure);
        assertEquals(0, productReads.get());
    }

    @Test
    public void assemblyAndDebugHooksWork() {
        AtomicInteger assemblies = new AtomicInteger();
        Hooks.onEachOperator("protocol-support-test", publisher -> {
            if (MonoProtocolSupport.class.isInstance(publisher)) {
                assemblies.incrementAndGet();
            }
            return publisher;
        });
        try {
            ProtocolSupport expected = protocol("test");
            StepVerifier.create(MonoProtocolSupport.create(
                            Mono.just(storage(() -> Mono.just(Value.simple("test")))),
                            supports(id -> Mono.just(expected)),
                            "protocol",
                            null))
                        .expectNext(expected)
                        .verifyComplete();
            assertEquals(1, assemblies.get());
        } finally {
            Hooks.resetOnEachOperator("protocol-support-test");
        }

        Hooks.onOperatorDebug();
        try {
            ProtocolSupport expected = protocol("test");
            StepVerifier.create(MonoProtocolSupport.create(
                            Mono.just(storage(() -> Mono.just(Value.simple("test")).hide())).hide(),
                            supports(id -> Mono.just(expected).hide()),
                            "protocol",
                            null))
                        .expectNext(expected)
                        .verifyComplete();

            List<String> keys = List.of("productId", "productVersion");
            Mono<DeviceProductOperator> fallbackProduct = MonoDeviceProduct.create(
                Mono.just(productStorage(requested -> Mono.just(Values.of(Map.of("productId", "prod"))))),
                registry((id, version) -> Mono.just(product(Mono.just(expected)))),
                keys,
                "productId",
                "productVersion"
            );
            StepVerifier.create(MonoProtocolSupport.create(
                            Mono.just(storage(Mono::empty)),
                            supports(id -> Mono.empty()),
                            "protocol",
                            fallbackProduct))
                        .expectNext(expected)
                        .verifyComplete();
        } finally {
            Hooks.resetOnOperatorDebug();
        }
    }

    private static ConfigStorage storage(Supplier<Mono<Value>> source) {
        return new InMemoryConfigStorage() {
            @Override
            public Mono<Value> getConfig(String key) {
                assertEquals("protocol", key);
                return source.get();
            }
        };
    }

    private static ConfigStorage productStorage(Function<Collection<String>, Mono<Values>> source) {
        return new InMemoryConfigStorage() {
            @Override
            public Mono<Values> getConfigs(Collection<String> keys) {
                return source.apply(keys);
            }
        };
    }

    private static DeviceRegistry registry(BiFunction<String, String, Mono<DeviceProductOperator>> lookup) {
        return (DeviceRegistry) Proxy.newProxyInstance(
            DeviceRegistry.class.getClassLoader(),
            new Class[]{DeviceRegistry.class},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), arguments);
                }
                if ("getProduct".equals(method.getName())) {
                    return lookup.apply((String) arguments[0], arguments.length == 2 ? (String) arguments[1] : null);
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static ProtocolSupports supports(Function<String, Mono<ProtocolSupport>> lookup) {
        return new ProtocolSupports() {
            @Override
            public boolean isSupport(String protocol) {
                return true;
            }

            @Override
            public Mono<ProtocolSupport> getProtocol(String protocol) {
                return lookup.apply(protocol);
            }

            @Override
            public Flux<ProtocolSupport> getProtocols() {
                return Flux.empty();
            }
        };
    }

    private static DeviceProductOperator product(Mono<ProtocolSupport> source) {
        return (DeviceProductOperator) Proxy.newProxyInstance(
            DeviceProductOperator.class.getClassLoader(),
            new Class[]{DeviceProductOperator.class},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), arguments);
                }
                if ("getProtocol".equals(method.getName())) {
                    return source;
                }
                if (Mono.class.isAssignableFrom(method.getReturnType())) {
                    return Mono.empty();
                }
                if (Flux.class.isAssignableFrom(method.getReturnType())) {
                    return Flux.empty();
                }
                return null;
            }
        );
    }

    private static ProtocolSupport protocol(String id) {
        return (ProtocolSupport) Proxy.newProxyInstance(
            ProtocolSupport.class.getClassLoader(),
            new Class[]{ProtocolSupport.class},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), arguments);
                }
                if ("getId".equals(method.getName())) {
                    return id;
                }
                if ("compareTo".equals(method.getName())) {
                    return id.compareTo(((ProtocolSupport) arguments[0]).getId());
                }
                if ("getOrder".equals(method.getName())) {
                    return 0;
                }
                if ("isDisposed".equals(method.getName())) {
                    return false;
                }
                if (Mono.class.isAssignableFrom(method.getReturnType())) {
                    return Mono.empty();
                }
                if (Flux.class.isAssignableFrom(method.getReturnType())) {
                    return Flux.empty();
                }
                return null;
            }
        );
    }

    private static Object objectMethod(Object proxy, String name, Object[] arguments) {
        if ("toString".equals(name)) {
            return proxy.getClass().getName();
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(name)) {
            return proxy == arguments[0];
        }
        throw new UnsupportedOperationException(name);
    }

    private static void verifyError(Mono<ProtocolSupport> source, Throwable expected) {
        StepVerifier.create(source)
                    .expectErrorMatches(error -> error == expected)
                    .verify(TIMEOUT);
    }
}
