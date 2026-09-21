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
        if (!fallbackParent) {
            return getReactiveStorage().flatMap(storage -> storage.getConfig(key));
        }
        return MonoConfigRead.create(getReactiveStorage(), this, key, fallbackParent);
    }

    @Override
    default <V> Mono<V> getConfig(ConfigKey<V> key) {
        return getConfig(key, true);
    }

    default <V> Mono<V> getConfig(ConfigKey<V> key, boolean fallbackParent) {
        return MonoConfigRead.create(getReactiveStorage(), this, key, fallbackParent);
    }

    default Mono<Values> getConfigs(Collection<String> keys, boolean fallbackParent) {
        return MonoConfigsRead.create(getReactiveStorage(), this, keys, fallbackParent);
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
