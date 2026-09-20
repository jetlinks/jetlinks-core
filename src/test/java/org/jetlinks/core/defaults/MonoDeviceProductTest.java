package org.jetlinks.core.defaults;

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
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MonoDeviceProductTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String PRODUCT_ID = "productId";
    private static final String PRODUCT_VERSION = "productVersion";

    @Test
    public void scalarReadIsLazyFreshAndDoesNotCacheProduct() {
        AtomicReference<String> currentVersion = new AtomicReference<>("v1");
        AtomicInteger storageReads = new AtomicInteger();
        AtomicInteger configReads = new AtomicInteger();
        AtomicInteger productReads = new AtomicInteger();
        DeviceProductOperator first = product("v1");
        DeviceProductOperator second = product("v2");
        DeviceRegistry registry = registry((id, version) -> {
            productReads.incrementAndGet();
            assertEquals("product", id);
            return Mono.just("v1".equals(version) ? first : second);
        });
        ConfigStorage storage = storage(() -> {
            configReads.incrementAndGet();
            return Mono.just(values(currentVersion.get()));
        });
        Mono<DeviceProductOperator> product = MonoDeviceProduct.create(
            Mono.fromSupplier(() -> {
                storageReads.incrementAndGet();
                return storage;
            }),
            registry,
            Arrays.asList(PRODUCT_ID, PRODUCT_VERSION),
            PRODUCT_ID,
            PRODUCT_VERSION
        );

        assertSame(first, product.block(TIMEOUT));
        currentVersion.set("v2");
        assertSame(second, product.block(TIMEOUT));
        assertEquals(2, storageReads.get());
        assertEquals(2, configReads.get());
        assertEquals(2, productReads.get());
        assertFalse(product instanceof Callable);
        assertFalse(product instanceof Fuseable);
    }

    @Test
    public void emptyStagesCompleteWithoutResolvingLaterStages() {
        AtomicInteger productReads = new AtomicInteger();
        DeviceRegistry registry = registry((id, version) -> {
            productReads.incrementAndGet();
            return Mono.empty();
        });
        StepVerifier.create(create(Mono.empty(), registry)).verifyComplete();
        StepVerifier.create(create(Mono.just(storage(Mono::empty)), registry)).verifyComplete();
        StepVerifier.create(create(Mono.just(storage(() -> Mono.just(Values.of(Map.of(PRODUCT_VERSION, "v1"))))), registry)).verifyComplete();
        StepVerifier.create(create(Mono.just(storage(() -> Mono.just(Values.of(Map.of(PRODUCT_ID, "   ", PRODUCT_VERSION, "v1"))))), registry)).verifyComplete();
        StepVerifier.create(create(Mono.just(storage(() -> Mono.just(values("v1")))), registry)).verifyComplete();
        assertEquals(1, productReads.get());
    }

    @Test
    public void asynchronousStagesPreserveContext() {
        DeviceProductOperator expected = product("v1");
        Mono<ConfigStorage> source = Mono.deferContextual(context -> {
            assertEquals("context", context.get("value"));
            return Mono.just(storage(() -> Mono.deferContextual(inner -> {
                assertEquals("context", inner.get("value"));
                return Mono.just(values("v1"));
            })));
        });
        DeviceRegistry registry = registry((id, version) -> Mono.deferContextual(context -> {
            assertEquals("context", context.get("value"));
            return Mono.just(expected);
        }));

        StepVerifier.create(create(source, registry).contextWrite(Context.of("value", "context")))
                    .expectNext(expected)
                    .verifyComplete();
    }

    @Test
    public void demandAndCancellationReachActiveSource() {
        DeviceProductOperator expected = product("v1");
        AtomicInteger storageReads = new AtomicInteger();
        AtomicInteger configReads = new AtomicInteger();
        AtomicInteger productReads = new AtomicInteger();
        Mono<DeviceProductOperator> source = create(
            Mono.fromSupplier(() -> {
                storageReads.incrementAndGet();
                return storage(() -> {
                    configReads.incrementAndGet();
                    return Mono.just(values("v1"));
                });
            }),
            registry((id, version) -> {
                productReads.incrementAndGet();
                return Mono.just(expected);
            })
        );
        StepVerifier.create(source, 0)
                    .expectSubscription()
                    .then(() -> assertEquals(0, storageReads.get()))
                    .then(() -> assertEquals(0, configReads.get()))
                    .then(() -> assertEquals(0, productReads.get()))
                    .thenRequest(1)
                    .expectNext(expected)
                    .verifyComplete();
        assertEquals(1, storageReads.get());
        assertEquals(1, configReads.get());
        assertEquals(1, productReads.get());

        StepVerifier.create(source, 0).thenCancel().verify(TIMEOUT);
        assertEquals(1, storageReads.get());

        AtomicInteger cancelled = new AtomicInteger();
        AtomicInteger pendingSubscriptions = new AtomicInteger();
        Sinks.One<DeviceProductOperator> pending = Sinks.one();
        StepVerifier.create(create(Mono.just(storage(() -> Mono.just(values("v1")))),
                                   registry((id, version) -> pending
                                       .asMono()
                                       .doOnSubscribe(ignore -> pendingSubscriptions.incrementAndGet())
                                       .doOnCancel(cancelled::incrementAndGet))), 0)
                    .expectSubscription()
                    .thenRequest(1)
                    .then(() -> assertEquals(1, pendingSubscriptions.get()))
                    .thenCancel()
                    .verify(TIMEOUT);
        assertEquals(1, cancelled.get());
    }

    @Test
    public void errorsDoNotResolveLaterStages() {
        IllegalStateException failure = new IllegalStateException("failure");
        AtomicInteger productReads = new AtomicInteger();
        DeviceRegistry registry = registry((id, version) -> {
            productReads.incrementAndGet();
            return Mono.empty();
        });
        verifyError(create(Mono.fromCallable(() -> {
            throw failure;
        }), registry), failure);
        verifyError(create(Mono.just(storage(() -> {
            throw failure;
        })), registry), failure);
        verifyError(create(Mono.just(storage(() -> Mono.error(failure))), registry), failure);
        assertEquals(0, productReads.get());

        verifyError(create(Mono.just(storage(() -> Mono.just(values("v1")))),
                           registry((id, version) -> Mono.error(failure))), failure);
    }

    @Test
    public void repeatedConcurrentSubscriptionsKeepIndependentReads() {
        AtomicInteger reads = new AtomicInteger();
        Mono<DeviceProductOperator> product = create(
            Mono.fromSupplier(() -> storage(() -> Mono.just(values(String.valueOf(reads.incrementAndGet()))))),
            registry((id, version) -> {
                return Mono.just(product(version));
            })
        );

        StepVerifier.create(Flux.range(0, 256)
                                .flatMap(index -> product.subscribeOn(Schedulers.parallel()), 8)
                                .map(DeviceProductOperator::getId)
                                .collectList())
                    .assertNext(values -> assertEquals(256, new HashSet<>(values).size()))
                    .verifyComplete();
        assertEquals(256, reads.get());
    }

    @Test
    public void assemblyAndDebugHooksWork() {
        AtomicInteger assemblies = new AtomicInteger();
        Hooks.onEachOperator("device-product-test", publisher -> {
            if (MonoDeviceProduct.class.isInstance(publisher)) {
                assemblies.incrementAndGet();
            }
            return publisher;
        });
        try {
            DeviceProductOperator expected = product("v1");
            StepVerifier.create(create(Mono.just(storage(() -> Mono.just(values("v1")))),
                                       registry((id, version) -> Mono.just(expected))))
                        .expectNext(expected)
                        .verifyComplete();
            assertEquals(1, assemblies.get());
        } finally {
            Hooks.resetOnEachOperator("device-product-test");
        }

        Hooks.onOperatorDebug();
        try {
            DeviceProductOperator expected = product("v1");
            StepVerifier.create(create(Mono.just(storage(() -> Mono.just(values("v1")).hide())).hide(),
                                       registry((id, version) -> Mono.just(expected).hide())))
                        .expectNext(expected)
                        .verifyComplete();
        } finally {
            Hooks.resetOnOperatorDebug();
        }
    }

    private static Mono<DeviceProductOperator> create(Mono<ConfigStorage> source, DeviceRegistry registry) {
        return MonoDeviceProduct.create(
            source,
            registry,
            Arrays.asList(PRODUCT_ID, PRODUCT_VERSION),
            PRODUCT_ID,
            PRODUCT_VERSION
        );
    }

    private static ConfigStorage storage(java.util.function.Supplier<Mono<Values>> values) {
        return new InMemoryConfigStorage() {
            @Override
            public Mono<Values> getConfigs(java.util.Collection<String> keys) {
                assertEquals(Arrays.asList(PRODUCT_ID, PRODUCT_VERSION), keys);
                return values.get();
            }
        };
    }

    private static Values values(String version) {
        return Values.of(Map.of(PRODUCT_ID, "product", PRODUCT_VERSION, version));
    }

    private static DeviceRegistry registry(ProductLookup lookup) {
        return (DeviceRegistry) Proxy.newProxyInstance(
            DeviceRegistry.class.getClassLoader(),
            new Class[]{DeviceRegistry.class},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), arguments);
                }
                if ("getProduct".equals(method.getName())) {
                    return lookup.get((String) arguments[0], arguments.length > 1 ? (String) arguments[1] : null);
                }
                if (Mono.class.isAssignableFrom(method.getReturnType())) {
                    return Mono.empty();
                }
                if (Flux.class.isAssignableFrom(method.getReturnType())) {
                    return Flux.empty();
                }
                throw new UnsupportedOperationException(method.toString());
            }
        );
    }

    private static DeviceProductOperator product(String id) {
        return (DeviceProductOperator) Proxy.newProxyInstance(
            DeviceProductOperator.class.getClassLoader(),
            new Class[]{DeviceProductOperator.class},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), arguments);
                }
                if ("getId".equals(method.getName())) {
                    return id;
                }
                if (Mono.class.isAssignableFrom(method.getReturnType())) {
                    return Mono.empty();
                }
                if (Flux.class.isAssignableFrom(method.getReturnType())) {
                    return Flux.empty();
                }
                throw new UnsupportedOperationException(method.toString());
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

    private static void verifyError(Mono<DeviceProductOperator> source, Throwable expected) {
        StepVerifier.create(source)
                    .expectErrorMatches(error -> error == expected)
                    .verify(TIMEOUT);
    }

    @FunctionalInterface
    private interface ProductLookup {
        Mono<DeviceProductOperator> get(String productId, String version);
    }
}
