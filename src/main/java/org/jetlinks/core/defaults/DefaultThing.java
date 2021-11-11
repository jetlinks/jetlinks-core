package org.jetlinks.core.defaults;

import lombok.Getter;
import org.jetlinks.core.Configurable;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.config.StorageConfigurable;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.things.*;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.jetlinks.core.device.DeviceConfigKey.metadata;
import static org.jetlinks.core.things.ThingsConfigKeys.lastMetadataTimeKey;

class DefaultThing implements Thing, StorageConfigurable {

    @Getter
    private final String id;

    @Getter
    private final ThingType type;

    private final Mono<ConfigStorage> storageMono;

    private final Mono<ThingMetadata> metadataMono;

    private final Mono<ThingTemplate> templateMono;

    private volatile long lastMetadataTime = -1;

    private volatile ThingMetadata metadataCache;

    private final ThingMetadataCodec metadataCodec;
    private  final Function<Thing,ThingRpcSupport> rpcFactory;
    public DefaultThing(ThingType thingType,
                        String id,
                        ConfigStorageManager storageManager,
                        ThingMetadataCodec metadataCodec,
                        ThingsRegistry registry,
                        Function<Thing,ThingRpcSupport> rpcFactory) {
        this(thingType,
             id,
             storageManager.getStorage("thing:" + thingType.getId() + ":" + id),
             metadataCodec,
             registry,
             rpcFactory);
    }

    public DefaultThing(ThingType thingType,
                        String id,
                        Mono<ConfigStorage> storageSupplier,
                        ThingMetadataCodec metadataCodec,
                        ThingsRegistry registry,
                        Function<Thing,ThingRpcSupport> rpcFactory) {
        this.id = id;
        this.type = thingType;
        this.storageMono = storageSupplier;
        this.metadataCodec = metadataCodec;
        this.rpcFactory=rpcFactory;
        this.templateMono = this
                .getSelfConfig(ThingsConfigKeys.templateId)
                .flatMap(templateId -> registry.getTemplate(this.type, templateId));

        this.metadataMono = this
                //获取最后更新物模型的时间
                .getSelfConfig(lastMetadataTimeKey.getKey())
                .map(Value::asLong)
                .flatMap(i -> {
                    //如果时间一致,则直接返回物模型缓存.
                    if (i.equals(lastMetadataTime) && metadataCache != null) {
                        return Mono.just(metadataCache);
                    }
                    lastMetadataTime = i;
                    //加载真实的物模型
                    return this
                            .getSelfConfig(metadata)
                            .flatMap(metadataCodec::decode)
                            .doOnNext(metadata -> metadataCache = metadata);

                })
                //获取物模版的物模型
                .switchIfEmpty(Mono.defer(() -> getTemplate().flatMap(ThingTemplate::getMetadata)));
    }

    @Override
    public Mono<? extends Configurable> getParent() {
        return getTemplate();
    }

    @Override
    public Mono<ConfigStorage> getReactiveStorage() {
        return storageMono;
    }

    @Override
    public Mono<ThingTemplate> getTemplate() {
        return templateMono;
    }

    @Override
    public Mono<ThingMetadata> getMetadata() {
        return metadataMono;
    }

    @Override
    public Mono<Boolean> updateMetadata(String metadata) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(DeviceConfigKey.metadata.getKey(), metadata);
        return setConfigs(configs);
    }

    @Override
    public Mono<Void> resetMetadata() {
        this.lastMetadataTime = -1;
        return removeConfigs(metadata, lastMetadataTimeKey)
                .then();
    }

    @Override
    public Mono<Boolean> setConfigs(Map<String, Object> conf) {
        Map<String, Object> configs = new HashMap<>(conf);
        if (conf.containsKey(metadata.getKey())) {
            configs.put(lastMetadataTimeKey.getKey(), lastMetadataTime = System.currentTimeMillis());
            return StorageConfigurable.super
                    .setConfigs(configs)
                    .doOnNext(suc -> this.metadataCache = null)
                    .thenReturn(true);
        }
        return StorageConfigurable.super.setConfigs(configs);
    }

    @Override
    public Mono<Boolean> updateMetadata(ThingMetadata metadata) {
        return this.metadataCodec
                .encode(metadata)
                .flatMap(this::updateMetadata);
    }

    @Override
    public Mono<Value> getSelfConfig(String key) {
        return getConfig(key, false);
    }

    @Override
    public Mono<Values> getSelfConfigs(Collection<String> keys) {
        return getConfigs(keys, false);
    }

    @Override
    public ThingRpcSupport rpc() {
        return this.rpcFactory.apply(this);
    }
}
