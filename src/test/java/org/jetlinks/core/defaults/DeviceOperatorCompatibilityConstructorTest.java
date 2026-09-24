package org.jetlinks.core.defaults;

import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceState;
import org.jetlinks.core.device.ProductInfo;
import org.jetlinks.core.device.StandaloneDeviceMessageBroker;
import org.jetlinks.core.device.TestConfigStorageManager;
import org.jetlinks.core.device.TestDeviceRegistry;
import org.jetlinks.core.device.TestProtocolSupport;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import org.jetlinks.core.metadata.DeviceMetadataCodec;
import org.jetlinks.core.metadata.SimpleDeviceMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class DeviceOperatorCompatibilityConstructorTest {

    @Test
    public void deviceCompatibilityConstructorsUseSameReactiveReads() {
        TestProtocolSupport support = metadataSupport();
        TestConfigStorageManager manager = new TestConfigStorageManager();
        StandaloneDeviceMessageBroker broker = new StandaloneDeviceMessageBroker();
        TestDeviceRegistry registry = new TestDeviceRegistry(support, broker, manager);
        List<DefaultDeviceOperator> operators = Arrays.asList(
            new DefaultDeviceOperator("default", support, manager, broker, registry),
            new DefaultDeviceOperator(
                "interceptor",
                support,
                manager,
                broker,
                registry,
                DeviceMessageSenderInterceptor.DO_NOTING
            ),
            new DefaultDeviceOperator(
                "state-checker",
                support,
                manager,
                broker,
                registry,
                DeviceMessageSenderInterceptor.DO_NOTING,
                device -> Mono.just(DeviceState.online)
            )
        );

        StepVerifier.create(
                        registry.register(ProductInfo.builder()
                                                     .id("product")
                                                     .protocol("test")
                                                     .metadata("product-metadata")
                                                     .build())
                                .thenMany(Flux.fromIterable(operators)
                                              .concatMap(operator -> operator
                                                  .setConfig(DeviceConfigKey.productId, "product")
                                                  .then(Mono.zip(
                                                      operator.getProduct(),
                                                      operator.getProtocol(),
                                                      operator.getMetadata()
                                                  ))))
                                .collectList())
                    .assertNext(results -> {
                        assertEquals(3, results.size());
                        results.forEach(result -> {
                            assertEquals("product", result.getT1().getId());
                            assertSame(support, result.getT2());
                            assertEquals("product-metadata", result.getT3().getProperties().get(0).getId());
                        });
                    })
                    .verifyComplete();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void productCompatibilityConstructorsUseSameReactiveReads() {
        TestProtocolSupport support = metadataSupport();
        TestConfigStorageManager manager = new TestConfigStorageManager();
        List<DefaultDeviceProductOperator> operators = Arrays.asList(
            new DefaultDeviceProductOperator("legacy", support, manager),
            new DefaultDeviceProductOperator("manager", support, manager, Flux::empty),
            new DefaultDeviceProductOperator(
                "storage",
                support,
                manager.getStorage("device-product:storage"),
                Flux::empty
            )
        );

        StepVerifier.create(Flux.fromIterable(operators)
                                .concatMap(operator -> {
                                    Map<String, Object> configs = new HashMap<>();
                                    configs.put(DeviceConfigKey.protocol.getKey(), "test");
                                    configs.put(DeviceConfigKey.metadata.getKey(), "product-metadata");
                                    return operator
                                        .setConfigs(configs)
                                        .then(Mono.zip(operator.getProtocol(), operator.getMetadata()));
                                })
                                .collectList())
                    .assertNext(results -> {
                        assertEquals(3, results.size());
                        results.forEach(result -> {
                            assertSame(support, result.getT1());
                            assertEquals("product-metadata", result.getT2().getProperties().get(0).getId());
                        });
                    })
                    .verifyComplete();
    }

    private static TestProtocolSupport metadataSupport() {
        return new TestProtocolSupport() {
            @Override
            public DeviceMetadataCodec getMetadataCodec() {
                return new DeviceMetadataCodec() {
                    @Override
                    public Mono<org.jetlinks.core.metadata.DeviceMetadata> decode(String source) {
                        SimpleDeviceMetadata metadata = new SimpleDeviceMetadata();
                        metadata.addProperty(SimplePropertyMetadata.of(source, source, null));
                        return Mono.just(metadata);
                    }

                    @Override
                    public Mono<String> encode(org.jetlinks.core.metadata.DeviceMetadata metadata) {
                        return Mono.empty();
                    }
                };
            }
        };
    }
}
