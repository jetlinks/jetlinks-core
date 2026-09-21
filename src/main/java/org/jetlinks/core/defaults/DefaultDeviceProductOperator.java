package org.jetlinks.core.defaults;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.Value;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.config.StorageConfigurable;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.things.ThingMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DefaultDeviceProductOperator implements DeviceProductOperator, StorageConfigurable {
    private static final MetadataState EMPTY_METADATA_STATE = new MetadataState(-1, null);

    @Getter
    private final String id;

    private volatile MetadataState metadataState = EMPTY_METADATA_STATE;

    @Getter(AccessLevel.PROTECTED)
    private final Mono<ConfigStorage> storageMono;

    private final Supplier<Flux<DeviceOperator>> devicesSupplier;

    private static final ConfigKey<Long> lastMetadataTimeKey = ConfigKey.of("lst_metadata_time");

    private final Mono<DeviceMetadata> metadataMono;

    private final Mono<ProtocolSupport> protocolSupportMono;

    @Deprecated
    public DefaultDeviceProductOperator(String id,
                                        ProtocolSupports supports,
                                        ConfigStorageManager manager) {
        this(id, supports, manager, Flux::empty);
    }

    public DefaultDeviceProductOperator(String id,
                                        ProtocolSupports supports,
                                        ConfigStorageManager manager,
                                        Supplier<Flux<DeviceOperator>> supplier) {
        this(id, supports, manager.getStorage("device-product:".concat(id)), supplier);
    }

    public DefaultDeviceProductOperator(String id,
                                        ProtocolSupports supports,
                                        Mono<ConfigStorage> storageMono,
                                        Supplier<Flux<DeviceOperator>> supplier) {
        this.id = id;
        this.storageMono = storageMono;
        this.devicesSupplier = supplier;
        this.protocolSupportMono = this
                .getConfig(DeviceConfigKey.protocol)
                .flatMap(supports::getProtocol);

        Mono<DeviceMetadata> loadMetadata = Mono
                .zip(
                        this.getProtocol().map(ProtocolSupport::getMetadataCodec),
                        this.getConfig(DeviceConfigKey.metadata),
                        this.getConfig(lastMetadataTimeKey)
                            .switchIfEmpty(Mono.defer(() -> {
                                long now = System.currentTimeMillis();
                                return this
                                        .setConfig(lastMetadataTimeKey, now)
                                        .thenReturn(now);
                            }))
                )
                .flatMap(tp3 -> tp3
                        .getT1()
                        .decode(tp3.getT2())
                        .doOnNext(decode -> {
                            this.metadataState = new MetadataState(tp3.getT3(), decode);
                        }));
        this.metadataMono = MonoVersionedMetadata.create(
            this.getConfig(lastMetadataTimeKey.getKey()),
            new MonoVersionedMetadata.Loader<Long, DeviceMetadata>() {
                @Override
                public Long convertVersion(Object value) {
                    return ((Value) value).as(Long.class);
                }

                @Override
                public Object currentSnapshot() {
                    return metadataState;
                }

                @Override
                public DeviceMetadata getCached(Object snapshot) {
                    return ((MetadataState) snapshot).metadata;
                }

                @Override
                public DeviceMetadata getCached() {
                    return metadataState.metadata;
                }

                @Override
                public boolean isSnapshotValid(Long time, Object snapshot) {
                    return time.equals(((MetadataState) snapshot).time);
                }

                @Override
                public boolean isValid(Long time, DeviceMetadata cached) {
                    MetadataState state = metadataState;
                    return cached == state.metadata && time.equals(state.time);
                }

                @Override
                public Mono<DeviceMetadata> load(Long time) {
                    return loadMetadata;
                }

                @Override
                public Mono<DeviceMetadata> loadEmpty() {
                    return loadMetadata;
                }
            }
        );
    }

    @Override
    public Mono<DeviceMetadata> getMetadata() {
        return this.metadataMono;

    }

    @Override
    public Mono<Boolean> updateMetadata(ThingMetadata metadata) {
        if (metadata instanceof DeviceMetadata) {
            return getProtocol()
                    .flatMap(protocol -> protocol.getMetadataCodec().encode((DeviceMetadata) metadata))
                    .flatMap(this::updateMetadata);
        }
        // FIXME: 2021/11/3
        return Mono.just(false);
    }

    @Override
    public Mono<Boolean> setConfigs(Map<String, Object> conf) {
        if (conf.containsKey(DeviceConfigKey.metadata.getKey())) {
            conf.put(lastMetadataTimeKey.getKey(), System.currentTimeMillis());
            return StorageConfigurable.super
                    .setConfigs(conf)
                    .doOnNext(s -> {
                        metadataState = new MetadataState((Long) conf.get(lastMetadataTimeKey.getKey()), null);
                    })
                    .then(this.getProtocol()
                              .flatMap(support -> support.onProductMetadataChanged(this))
                    )
                    .thenReturn(true);
        }
        return StorageConfigurable.super.setConfigs(conf);
    }

    @Override
    public Mono<Boolean> updateMetadata(String metadata) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(DeviceConfigKey.metadata.getKey(), metadata);
        return this.setConfigs(configs);
    }

    @Override
    public Mono<ProtocolSupport> getProtocol() {
        return protocolSupportMono;
    }

    @Override
    public Mono<ConfigStorage> getReactiveStorage() {
        return storageMono;
    }

    @Override
    public Flux<DeviceOperator> getDevices() {
        return devicesSupplier == null ? Flux.empty() : devicesSupplier.get();
    }

    private static final class MetadataState {
        private final long time;
        private final DeviceMetadata metadata;

        private MetadataState(long time, DeviceMetadata metadata) {
            this.time = time;
            this.metadata = metadata;
        }
    }
}
