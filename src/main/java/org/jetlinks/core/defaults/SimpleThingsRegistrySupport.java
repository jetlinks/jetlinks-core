package org.jetlinks.core.defaults;

import com.google.common.collect.Maps;
import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.things.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class SimpleThingsRegistrySupport implements ThingsRegistrySupport {

    private final ThingType thingType;

    //缓存
    private final Map<String, DefaultThing> thingCache = new ConcurrentHashMap<>();
    private final Map<String, DefaultThingTemplate> templateCache = new ConcurrentHashMap<>();

    private final Mono<ConfigStorage> registryInfo;
    private final Mono<ConfigStorage> templateRegistryInfo;

    //配置管理器
    private final ConfigStorageManager manager;

    private final ThingMetadataCodec metadataCodec;

    private final Function<Thing, ThingRpcSupport> rpcSupportFactory;

    public SimpleThingsRegistrySupport(ThingType thingType,
                                       ConfigStorageManager manager,
                                       ThingMetadataCodec metadataCodec,
                                       Function<Thing, ThingRpcSupport> rpcSupportFactory) {
        this.thingType = thingType;
        this.manager = manager;
        this.metadataCodec = metadataCodec;
        this.rpcSupportFactory = rpcSupportFactory;
        registryInfo = manager.getStorage("thing_reg:" + thingType.getId());
        templateRegistryInfo = manager.getStorage("thing_temp_reg:" + thingType.getId());
    }

    @Override
    public boolean isSupported(String thingType) {
        return this.thingType.getId().equals(thingType);
    }

    public void checkThingType(String thingType) {
        if (!isSupported(thingType)) {
            throw new UnsupportedOperationException("unsupported thing type : " + thingType);
        }
    }

    @Override
    public Mono<Thing> getThing(@Nonnull String thingType, @Nonnull String thingId) {
        checkThingType(thingType);
        return registryInfo
                .flatMap(storage -> storage.getConfig(thingId))
                .switchIfEmpty(Mono.fromRunnable(() -> thingCache.remove(thingId)))
                .map(ignore -> thingCache.computeIfAbsent(thingId, this::createThing));
    }

    protected DefaultThing createThing(String id) {
        return new DefaultThing(thingType, id, manager, metadataCodec, this, rpcSupportFactory);
    }

    protected DefaultThingTemplate createTemplate(String id) {
        return new DefaultThingTemplate(thingType, id, manager, metadataCodec);
    }

    @Override
    public Mono<Thing> register(@Nonnull String thingType, @Nonnull ThingInfo info) {
        checkThingType(thingType);
        DefaultThing thing = createThing(info.getId());

        Map<String, Object> configs = Maps.newHashMap();
        Optional.ofNullable(info.getConfiguration())
                .ifPresent(configs::putAll);
        Optional.ofNullable(info.getMetadata())
                .ifPresent(conf -> configs.put(ThingsConfigKeys.metadata.getKey(), conf));
        Optional.ofNullable(info.getVersion())
                .ifPresent(conf -> configs.put(ThingsConfigKeys.version.getKey(), conf));
        Optional.ofNullable(info.getTemplateId())
                .ifPresent(conf -> configs.put(ThingsConfigKeys.templateId.getKey(), conf));
        Optional.ofNullable(info.getName())
                .ifPresent(conf -> configs.put(ThingsConfigKeys.name.getKey(), conf));

        //FIXME 版本比对?
        return registryInfo
                .flatMap(storage -> storage.setConfig(info.getId(), System.currentTimeMillis()))
                .then(thing.setConfigs(configs))
                .thenReturn(thing);
    }

    @Override
    public Mono<ThingTemplate> getTemplate(@Nonnull String thingType, @Nonnull String templateId) {
        checkThingType(thingType);
        return templateRegistryInfo
                .flatMap(storage -> storage.getConfig(templateId))
                .switchIfEmpty(Mono.fromRunnable(() -> templateCache.remove(templateId)))
                .map(ignore -> templateCache.computeIfAbsent(templateId, this::createTemplate));
    }

    @Override
    public Mono<Void> unregisterThing(@Nonnull String thingType, @Nonnull String thingId) {
        checkThingType(thingType);
        return Flux
                .merge(
                        Mono.justOrEmpty(thingCache.remove(thingId))
                            .flatMap(DefaultThing::getReactiveStorage)
                            .flatMap(ConfigStorage::clear)
                        ,
                        registryInfo
                                .flatMap(storage -> storage.remove(thingId))

                )
                .then();
    }

    @Override
    public Mono<ThingTemplate> register(@Nonnull String thingType, @Nonnull ThingTemplateInfo info) {
        checkThingType(thingType);
        DefaultThingTemplate thing = createTemplate(info.getId());

        Map<String, Object> configs = new HashMap<>();
        Optional.ofNullable(info.getConfiguration())
                .ifPresent(configs::putAll);
        Optional.ofNullable(info.getMetadata())
                .ifPresent(conf -> configs.put(ThingsConfigKeys.metadata.getKey(), conf));
        Optional.ofNullable(info.getVersion())
                .ifPresent(conf -> configs.put(ThingsConfigKeys.version.getKey(), conf));
        Optional.ofNullable(info.getType())
                .ifPresent(conf -> configs.put(ThingsConfigKeys.type.getKey(), conf));
        Optional.ofNullable(info.getName())
                .ifPresent(conf -> configs.put(ThingsConfigKeys.name.getKey(), conf));

        //FIXME 版本比对?
        return templateRegistryInfo
                .flatMap(storage -> storage.setConfig(info.getId(), System.currentTimeMillis()))
                .then(thing.setConfigs(configs))
                .thenReturn(thing);
    }

    @Override
    public Mono<Void> unregisterTemplate(@Nonnull String thingType, @Nonnull String thingId) {
        checkThingType(thingType);
        return Flux
                .merge(
                        Mono.justOrEmpty(templateCache.remove(thingId))
                            .flatMap(DefaultThingTemplate::getReactiveStorage)
                            .flatMap(ConfigStorage::clear)
                        ,
                        templateRegistryInfo
                                .flatMap(storage -> storage.remove(thingId))

                )
                .then();
    }


}
