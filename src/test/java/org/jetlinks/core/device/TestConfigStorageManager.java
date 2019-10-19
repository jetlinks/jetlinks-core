package org.jetlinks.core.device;

import org.jetlinks.core.config.ConfigStorage;
import org.jetlinks.core.config.ConfigStorageManager;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestConfigStorageManager implements ConfigStorageManager {

    private Map<String, ConfigStorage> storageMap = new ConcurrentHashMap<>();

    @Override
    public Mono<ConfigStorage> getStorage(String id) {
        return Mono.just(storageMap.computeIfAbsent(id, __ -> new TestConfigStorage()));
    }
}
