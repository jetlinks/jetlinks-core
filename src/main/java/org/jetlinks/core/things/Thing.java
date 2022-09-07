package org.jetlinks.core.things;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.core.Wrapper;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.device.DeviceConfigKey;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 物实例接口，可通过此接口获取物的物模型以及相关配置信息.
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface Thing extends Configurable, Wrapper {

    /**
     * @return 物ID
     */
    String getId();

    /**
     * @return 物类型
     */
    ThingType getType();

    /**
     * 获取当前物使用的模版
     *
     * @return 当前物使用的模版
     */
    Mono<? extends ThingTemplate> getTemplate();

    /**
     * 重置物的物模型,重置后物模型将使用模版的物模型
     *
     * @return void
     */
    Mono<Void> resetMetadata();

    /**
     * 获取当前物的物模型,如果当前物没有单独配置物模型,则获取模版里的物模型
     *
     * @return 物模型
     */
    Mono<? extends ThingMetadata> getMetadata();

    /**
     * 更新物模型
     *
     * @param metadata 物模型json
     * @return true
     */
    Mono<Boolean> updateMetadata(String metadata);

    /**
     * 更新物模型
     *
     * @param metadata 物模型对象
     * @return true
     */
    Mono<Boolean> updateMetadata(ThingMetadata metadata);

    /**
     * 获取自身的配置,如果配置不存在则返回{@link Mono#empty()}
     *
     * @param key 配置Key
     * @return 配置值
     */
    Mono<Value> getSelfConfig(String key);

    /**
     * 获取自身的多个配置,不会返回{@link Mono#empty()},通过从{@link Values}中获取对应的值
     *
     * @param keys 配置key列表
     * @return 配置值
     */
    Mono<Values> getSelfConfigs(Collection<String> keys);

    /**
     * 获取自身的多个配置
     *
     * @param keys 配置key列表
     * @return 配置值
     */
    default Mono<Values> getSelfConfigs(String... keys) {
        return getSelfConfigs(Arrays.asList(keys));
    }

    /**
     * 获取自身的配置
     *
     * @param key 配置key
     * @return 配置值
     * @see DeviceConfigKey
     */
    default <V> Mono<V> getSelfConfig(ConfigKey<V> key) {
        return getSelfConfig(key.getKey())
                .map(value -> value.as(key.getValueType()));
    }

    /**
     * 获取自身的多个配置
     *
     * @param keys 配置key
     * @return 配置值
     * @see DeviceConfigKey
     */
    default Mono<Values> getSelfConfigs(ConfigKey<?>... keys) {
        return getSelfConfigs(Arrays.stream(keys).map(ConfigKey::getKey).collect(Collectors.toSet()));
    }

    /**
     * 获取RPC操作接口
     *
     * @return ThingRpcSupport
     */
    default ThingRpcSupport rpc() {
        return (msg) -> Flux.error(UnsupportedOperationException::new);
    }
}
