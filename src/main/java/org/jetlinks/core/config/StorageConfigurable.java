package org.jetlinks.core.config;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

/**
 * 使用{@link ConfigStorage}来提供配置支持
 *
 * @author zhouhao
 * @see ConfigStorage
 * @see ConfigStorageManager
 * @since 1.0
 */
public interface StorageConfigurable extends Configurable {

    /**
     * 异步获取配置器
     *
     * @return ConfigStorage
     */
    Mono<ConfigStorage> getReactiveStorage();

    /**
     * 获取上级配置器
     *
     * @return Configurable
     */
    default Mono<? extends Configurable> getParent() {
        return Mono.empty();
    }

    @Override
    default Mono<Value> getConfig(String key) {
        return getConfig(key, true);
    }

    default Mono<Value> getConfig(String key, boolean fallbackParent) {
        if (fallbackParent) {
            return getReactiveStorage()
                .flatMap(store -> store.getConfig(key))
                .switchIfEmpty(Mono.defer(() -> getParent().flatMap(parent -> parent.getConfig(key))));
        }
        return getReactiveStorage().flatMap(store -> store.getConfig(key));
    }

    default Mono<Values> getConfigs(Collection<String> keys, boolean fallbackParent) {
        if (!fallbackParent) {
            return getReactiveStorage()
                .flatMap(store -> store.getConfigs(keys));
        }
        return getReactiveStorage()
            .flatMap(store -> store.getConfigs(keys))
            .flatMap(values -> {
                int keySize = keys.size();
                //尝试获取上一级的配置
                if (keySize > 0 && values.size() != keySize) {
                    Collection<String> nonExistent = values.getNonExistentKeys(keys);
                    return getParent()
                        .flatMap(parent -> parent.getConfigs(nonExistent))
                        .map(parentValues -> parentValues.merge(values))
                        .defaultIfEmpty(values);
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
    default Mono<Value> getAndRemoveConfig(String key) {
        return getReactiveStorage()
            .flatMap(storage -> storage.getAndRemove(key));
    }

    @Override
    default Mono<Boolean> removeConfigs(Collection<String> key) {
        return getReactiveStorage()
            .flatMap(storage -> storage.remove(key));
    }

    @Override
    default Mono<Void> refreshConfig(Collection<String> keys) {
        return getReactiveStorage()
            .flatMap(storage -> storage.refresh(keys));
    }

    @Override
    default Mono<Void> refreshAllConfig() {
        return getReactiveStorage()
            .flatMap(ConfigStorage::refresh);
    }
}
