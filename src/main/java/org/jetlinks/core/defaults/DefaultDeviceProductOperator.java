package org.jetlinks.core.defaults;

import lombok.Getter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.Value;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.config.StorageConfigurable;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

public class DefaultDeviceProductOperator implements DeviceProductOperator, StorageConfigurable {

    @Getter
    private final String id;

    private final ProtocolSupports protocolSupports;

    private final ConfigStorageManager storageManager;

    private AtomicReference<DeviceMetadata> metadataCache = new AtomicReference<>();


    public DefaultDeviceProductOperator(String id,
                                        ProtocolSupports supports,
                                        ConfigStorageManager manager) {
        this.id = id;
        this.protocolSupports = supports;
        this.storageManager = manager;
    }

    @Override
    public Mono<DeviceMetadata> getMetadata() {
        return Mono.justOrEmpty(metadataCache.get())
                .switchIfEmpty(getProtocol()
                        .flatMap(protocol -> getConfig(DeviceConfigKey.metadata)
                                .flatMap(protocol.getMetadataCodec()::decode)
                                .doOnNext(metadataCache::set)));
    }

    @Override
    public Mono<Boolean> updateMetadata(String metadata) {
        return setConfig(DeviceConfigKey.metadata.value(metadata))
                .doOnSuccess((v) -> metadataCache.set(null));
    }


    @Override
    public Mono<ProtocolSupport> getProtocol() {
        return getConfig(DeviceConfigKey.protocol)
                .flatMap(protocolSupports::getProtocol);
    }

    @Override
    public Mono<ConfigStorage> getReactiveStorage() {
        return storageManager
                .getStorage("device-product:".concat(id));
    }
}
