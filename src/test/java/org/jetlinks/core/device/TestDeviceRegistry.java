package org.jetlinks.core.device;

import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.defaults.DefaultDeviceOperator;
import org.jetlinks.core.defaults.DefaultDeviceProductOperator;
import org.jetlinks.core.message.interceptor.DeviceMessageSenderInterceptor;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TestDeviceRegistry implements DeviceRegistry {

    private CompositeDeviceMessageSenderInterceptor interceptor = new CompositeDeviceMessageSenderInterceptor();

    private ConfigStorageManager manager = new TestConfigStorageManager();

    private Map<String, DeviceOperator> operatorMap = new ConcurrentHashMap<>();

    private Map<String, DeviceProductOperator> productOperatorMap = new ConcurrentHashMap<>();

    private ProtocolSupports supports;

    private DeviceOperationBroker handler;

    public TestDeviceRegistry(ProtocolSupports supports, DeviceOperationBroker handler) {
        this.supports = supports;
        this.handler = handler;
    }

    @Override
    public Mono<DeviceOperator> getDevice(String deviceId) {
        return Mono.fromSupplier(() -> deviceId == null ? null : operatorMap.get(deviceId));
    }

    @Override
    public Mono<DeviceProductOperator> getProduct(String productId) {
        return Mono.fromSupplier(() -> productId == null ? null : productOperatorMap.get(productId));
    }

    @Override
    public Mono<DeviceOperator> register(DeviceInfo deviceInfo) {
        return Mono.defer(() -> {
            DefaultDeviceOperator operator = new DefaultDeviceOperator(
                deviceInfo.getId(),
                supports, manager, handler, this, interceptor
            );
            operatorMap.put(operator.getDeviceId(), operator);

            Map<String, Object> configs = new HashMap<>();
            Optional.ofNullable(deviceInfo.getMetadata())
                    .ifPresent(conf -> configs.put(DeviceConfigKey.metadata.getKey(), conf));
            Optional.ofNullable(deviceInfo.getProtocol())
                    .ifPresent(conf -> configs.put(DeviceConfigKey.protocol.getKey(), conf));
            Optional.ofNullable(deviceInfo.getProductId())
                    .ifPresent(conf -> configs.put(DeviceConfigKey.productId.getKey(), conf));

            Optional.ofNullable(deviceInfo.getConfiguration())
                    .ifPresent(configs::putAll);

            return operator.setConfigs(configs).thenReturn(operator);
        });
    }

    @Override
    public Mono<DeviceProductOperator> register(ProductInfo productInfo) {
        return Mono.defer(() -> {
            DefaultDeviceProductOperator operator = new DefaultDeviceProductOperator(productInfo.getId(), supports, manager);
            productOperatorMap.put(operator.getId(), operator);
            Map<String, Object> configs = new HashMap<>();
            Optional.ofNullable(productInfo.getMetadata())
                    .ifPresent(conf -> configs.put(DeviceConfigKey.metadata.getKey(), conf));
            Optional.ofNullable(productInfo.getProtocol())
                    .ifPresent(conf -> configs.put(DeviceConfigKey.protocol.getKey(), conf));

            Optional.ofNullable(productInfo.getConfiguration())
                    .ifPresent(configs::putAll);

            return operator.setConfigs(configs).thenReturn(operator);
        });
    }

    @Override
    public Mono<Void> unregisterDevice(String deviceId) {
        return Mono.justOrEmpty(deviceId)
                   .map(operatorMap::remove)
                   .then();
    }

    @Override
    public Mono<Void> unregisterProduct(String productId) {
        return Mono.justOrEmpty(productId)
                   .map(productOperatorMap::remove)
                   .then();
    }

    public void addInterceptor(DeviceMessageSenderInterceptor interceptor) {
        this.interceptor.addInterceptor(interceptor);
    }
}
