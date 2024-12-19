package org.jetlinks.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.config.ConfigKeyValue;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 可配置接口
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface Configurable {

    /**
     * 获取配置,如果值不存在则返回{@link Mono#empty()}
     *
     * @param key key
     * @return 结果包装器, 不会为null
     * @see Value#get()
     */
    Mono<Value> getConfig(String key);

    /**
     * 获取多个配置信息
     *
     * @param keys 配置key集合
     * @return 配置信息
     */
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
                .mapNotNull(v->v.as(key.getValueType()));
    }

    default Mono<Values> getConfigs(ConfigKey<?>... key) {
        Set<String> keys = Sets.newHashSetWithExpectedSize(key.length);
        for (ConfigKey<?> configKey : key) {
            keys.add(configKey.getKey());
        }
        return getConfigs(keys);
    }

    /**
     * 获取多个配置,如果未指定key,则获取全部配置
     *
     * @return 所有配置结果集合
     */
    default Mono<Values> getConfigs(String... keys) {
        return getConfigs(Sets.newHashSet(keys));
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
     *
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

    /**
     * 刷新配置信息
     *
     * @return key
     */
    Mono<Void> refreshConfig(Collection<String> keys);

    /**
     * 刷新全部配置信息
     *
     * @return key
     */
    Mono<Void> refreshAllConfig();

    /**
     * 删除多个配置信息
     *
     * @param key key
     * @return 删除结果
     */
    default Mono<Boolean> removeConfigs(ConfigKey<?>... key) {
        return removeConfigs(Arrays.stream(key).map(ConfigKey::getKey).collect(Collectors.toSet()));
    }


}
