package org.jetlinks.core;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.config.ConfigKeyValue;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 可配置接口
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface Configurable {

    /**
     * 获取配置
     *
     * @param key key
     * @return 结果包装器, 不会为null
     * @see Value#get()
     */
    Mono<Value> getConfig(String key);

    Mono<Values> getConfigs(Collection<String> keys);

    /**
     * 设置一个配置,配置最好以基本数据类型或者json为主
     *
     * @param key   配置key
     * @param value 值 不能为null
     */
    Mono<Boolean> setConfig(String key, Object value);

    default Mono<Boolean> setConfig(ConfigKeyValue<?> keyValue) {
        return setConfig(keyValue.getKey(), keyValue.getValue());
    }

    default <T> Mono<Boolean> setConfig(ConfigKey<T> key, T value) {
        return setConfig(key.getKey(), value);
    }

    default Mono<Boolean> setConfigs(ConfigKeyValue<?>... keyValues) {
        return setConfigs(Arrays.stream(keyValues)
                .filter(ConfigKeyValue::isNotNull)
                .collect(Collectors.toMap(ConfigKeyValue::getKey, ConfigKeyValue::getValue)));
    }

    default <V> Mono<V> getConfig(ConfigKey<V> key) {
        return getConfig(key.getKey())
                .flatMap(value -> Mono.justOrEmpty(value.as(key.getType())));
    }

    default Mono<Values> getConfigs(ConfigKey<?>... key) {
        return getConfigs(Arrays.stream(key)
                .map(ConfigKey::getKey)
                .collect(Collectors.toSet()));
    }

    /**
     * 获取多个配置,如果未指定key,则获取全部配置
     *
     * @return 所有配置结果集合
     */
    default Mono<Values> getConfigs(String... keys) {
        return getConfigs(Arrays.asList(keys));
    }

    /**
     * 批量设置配置
     *
     * @param conf 配置内容
     */
    Mono<Boolean> setConfigs(Map<String, Object> conf);

    /**
     * 删除配置
     *
     * @param key key
     */
    Mono<Boolean> removeConfig(String key);

    /**
     * 获取并删除配置
     * @param key key
     * @return 被删除的配置
     * @since 1.1.1
     */
    Mono<Value> getAndRemoveConfig(String key);

    /**
     * 删除配置
     *
     * @param key key
     * @return 被删除的值，不存在则返回empty
     */
    Mono<Boolean> removeConfigs(Collection<String> key);

    default Mono<Boolean> removeConfigs(ConfigKey<?>... key) {
        return removeConfigs(Arrays.stream(key).map(ConfigKey::getKey).collect(Collectors.toSet()));
    }


}
