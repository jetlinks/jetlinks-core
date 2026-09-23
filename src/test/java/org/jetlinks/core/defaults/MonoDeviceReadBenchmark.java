package org.jetlinks.core.defaults;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.InMemoryConfigStorage;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MonoDeviceReadBenchmark {

    private Mono<DeviceProductOperator> product;
    private Mono<ProtocolSupport> directProtocol;
    private Mono<ProtocolSupport> fallbackProtocol;

    @Setup
    public void setup() {
        ProtocolSupport support = proxy(ProtocolSupport.class);
        DeviceProductOperator productOperator = product(support);
        DeviceRegistry registry = registry(productOperator);
        List<String> keys = List.of("productId", "productVersion");
        ConfigStorage productStorage = new InMemoryConfigStorage() {
            @Override
            public Mono<Values> getConfigs(java.util.Collection<String> ignored) {
                return Mono.just(Values.of(Map.of("productId", "product", "productVersion", "v1")));
            }
        };
        product = MonoDeviceProduct.create(
            Mono.just(productStorage),
            registry,
            keys,
            "productId",
            "productVersion"
        );

        ProtocolSupports supports = new ProtocolSupports() {
            @Override
            public boolean isSupport(String protocol) {
                return true;
            }

            @Override
            public Mono<ProtocolSupport> getProtocol(String protocol) {
                return "direct".equals(protocol) ? Mono.just(support) : Mono.empty();
            }

            @Override
            public Flux<ProtocolSupport> getProtocols() {
                return Flux.empty();
            }
        };
        ConfigStorage directStorage = new InMemoryConfigStorage() {
            @Override
            public Mono<Value> getConfig(String key) {
                return Mono.just(Value.simple("direct"));
            }
        };
        ConfigStorage fallbackStorage = new InMemoryConfigStorage() {
            @Override
            public Mono<Value> getConfig(String key) {
                return Mono.empty();
            }
        };
        directProtocol = MonoProtocolSupport.create(
            Mono.just(directStorage),
            supports,
            "protocol",
            product
        );
        fallbackProtocol = MonoProtocolSupport.create(
            Mono.just(fallbackStorage),
            supports,
            "protocol",
            product
        );
    }

    @Benchmark
    public DeviceProductOperator product() {
        return product.block();
    }

    @Benchmark
    public ProtocolSupport directProtocol() {
        return directProtocol.block();
    }

    @Benchmark
    public ProtocolSupport fallbackProtocol() {
        return fallbackProtocol.block();
    }

    private static DeviceRegistry registry(DeviceProductOperator product) {
        return (DeviceRegistry) Proxy.newProxyInstance(
            DeviceRegistry.class.getClassLoader(),
            new Class[]{DeviceRegistry.class},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), arguments);
                }
                if ("getProduct".equals(method.getName())) {
                    return Mono.just(product);
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

    private static DeviceProductOperator product(ProtocolSupport support) {
        return (DeviceProductOperator) Proxy.newProxyInstance(
            DeviceProductOperator.class.getClassLoader(),
            new Class[]{DeviceProductOperator.class},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), arguments);
                }
                if ("getProtocol".equals(method.getName())) {
                    return Mono.just(support);
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

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class[]{type},
            (proxy, method, arguments) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return objectMethod(proxy, method.getName(), arguments);
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
}
