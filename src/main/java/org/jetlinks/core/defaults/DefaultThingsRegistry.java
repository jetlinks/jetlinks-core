package org.jetlinks.core.defaults;

import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.things.Thing;
import org.jetlinks.core.things.ThingInfo;
import org.jetlinks.core.things.ThingMetadataCodec;
import org.jetlinks.core.things.ThingsRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultThingsRegistry implements ThingsRegistry {

    //缓存
    private final Map<String, DefaultThing> thingCache = new ConcurrentHashMap<>();

    private final Mono<ConfigStorage> registryInfo;

    //配置管理器
    private final ConfigStorageManager manager;

    private final ThingMetadataCodec metadataCodec;

    public DefaultThingsRegistry(ConfigStorageManager manager,
                                 ThingMetadataCodec metadataCodec) {
        this.manager = manager;
        this.metadataCodec = metadataCodec;
        registryInfo = manager.getStorage("thing_registry");
    }

    @Override
    public Mono<Thing> getThing(String thingId) {

        return registryInfo
                .flatMap(storage -> storage.getConfig(thingId))
                .switchIfEmpty(Mono.fromRunnable(() -> thingCache.remove(thingId)))
                .map(ignore -> thingCache.computeIfAbsent(thingId, (id) -> new DefaultThing(id, manager, metadataCodec)));
    }

    @Override
    public Mono<Thing> register(ThingInfo info) {
        DefaultThing thing = new DefaultThing(info.getId(), manager, metadataCodec);

        Map<String, Object> configs = new HashMap<>();
        Optional.ofNullable(info.getConfiguration())
                .ifPresent(configs::putAll);
        Optional.ofNullable(info.getMetadata())
                .ifPresent(conf -> configs.put(DeviceConfigKey.metadata.getKey(), conf));

        return registryInfo
                .flatMap(storage -> storage.setConfig(info.getId(), System.currentTimeMillis()))
                .then(thing.setConfigs(configs))
                .thenReturn(thing);
    }

    @Override
    public Mono<Void> unregister(String thingId) {
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


}
