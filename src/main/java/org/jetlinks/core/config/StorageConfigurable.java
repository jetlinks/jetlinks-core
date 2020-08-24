package org.jetlinks.core.config;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface StorageConfigurable extends Configurable {

    Mono<ConfigStorage> getReactiveStorage();

    default Mono<? extends Configurable> getParent() {
        return Mono.empty();
    }

    @Override
    default Mono<Value> getConfig(String key) {
        return getConfig(key, true);
    }

    default Mono<Value> getConfig(String key, boolean fallbackParent) {
        return getReactiveStorage()
                .flatMap(store -> store.getConfig(key))
                .switchIfEmpty(Mono.defer(() -> fallbackParent ? getParent().flatMap(parent -> parent.getConfig(key)) : Mono.empty()));
    }

    default Mono<Values> getConfigs(Collection<String> keys, boolean fallbackParent) {
        return getReactiveStorage()
                .flatMap(store -> store.getConfigs(keys))
                .flatMap(values -> {
                    //尝试获取上一级的配置
                    if (!keys.isEmpty() && values.size() != keys.size() && fallbackParent) {
                        Set<String> nonExistent = values.getNonExistentKeys(keys);
                        return getParent()
                                .flatMap(parent -> parent.getConfigs(nonExistent))
                                .map(parentValues -> parentValues.merge(values))
                                .switchIfEmpty(Mono.just(values));
                    }
                    return Mono.just(values);
                });
    }

    @Override
    default Mono<Values> getConfigs(Collection<String> keys) {
        return getConfigs(keys, true);
    }

    @Override
    default Mono<Boolean> setConfig(String key, Object value) {
        return getReactiveStorage()
                .flatMap(store -> store.setConfig(key, value));
    }

    @Override
    default Mono<Boolean> setConfigs(Map<String, Object> conf) {
        return getReactiveStorage()
                .flatMap(storage -> storage.setConfigs(conf));
    }

    @Override
    default Mono<Boolean> removeConfig(String key) {
        return getReactiveStorage()
                .flatMap(storage -> storage.remove(key));
    }

    @Override
   default Mono<Value> getAndRemoveConfig(String key){
        return getReactiveStorage()
                .flatMap(storage -> storage.getAndRemove(key));
    }

    @Override
    default Mono<Boolean> removeConfigs(Collection<String> key) {
        return getReactiveStorage()
                .flatMap(storage -> storage.remove(key));
    }
}
