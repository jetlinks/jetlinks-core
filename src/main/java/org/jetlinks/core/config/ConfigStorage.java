package org.jetlinks.core.config;

import com.google.common.collect.Sets;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;

/**
 * 配置存储器，用于定义配置存储接口以及常用方法
 *
 * @author zhouhao
 * @since 1.0
 */
public interface ConfigStorage {

    /**
     * 获取单个值,如果值不存在,则返回{@link  Mono#empty()},可通过{@link  Mono#switchIfEmpty(Mono)}来处理值不存在的情况.
     *
     * @param key key
     * @return Value
     */
    Mono<Value> getConfig(String key);

    /**
     * 获取配置,如果值不存在,则使用指定的加载器进行加载.
     *
     * @param key    key
     * @param loader 默认值加载器
     * @return Value
     */
    default Mono<Value> getConfig(String key, Mono<Object> loader) {
        return this
            .getConfig(key)
            .switchIfEmpty(loader.map(Value::simple));
    }

    /**
     * 获取多个值,参照{@link ConfigStorage#getConfigs(Collection)}
     *
     * @param keys keys
     * @return 多个值信息
     */
    default Mono<Values> getConfigs(String... keys) {
        return getConfigs(Sets.newHashSet(keys));
    }

    /**
     * 获取多个key对应的值,此方法不会返回{@link Mono#empty()},当值都不存在时，可以通过{@link  Values#isEmpty()}来判断.
     *
     * @param keys keys
     * @return Values
     * @see Values
     */
    Mono<Values> getConfigs(Collection<String> keys);

    /**
     * 设置多个值到配置中,Map中的value应该为可序列化的对象,最好为基本数据类型,字符串类型.
     *
     * @param values 多个值
     * @return 是否成功
     */
    Mono<Boolean> setConfigs(Map<String, Object> values);

    /**
     * 设置单个配置,如果值已经存在则会被覆盖.value应该为可序列化的对象,最好为基本数据类型,字符串类型.
     *
     * @param key   key
     * @param value 值
     * @return 是否成功
     */
    Mono<Boolean> setConfig(String key, Object value);

    /**
     * 根据key删除单个配置值
     *
     * @param key key
     * @return 是否成功
     */
    Mono<Boolean> remove(String key);

    /**
     * 获取值然后后删除对应的值,如果值不存在则返回{@link  Mono#empty()}
     *
     * @param key key
     * @return 值对象
     */
    Mono<Value> getAndRemove(String key);

    /**
     * 根据key删除多个值
     *
     * @param keys keys
     * @return 是否成功
     */
    Mono<Boolean> remove(Collection<String> keys);

    /**
     * 清空全部配置
     *
     * @return 是否成功
     */
    Mono<Boolean> clear();

    /**
     * 刷新指定keys缓存信息,通常用于在二级缓存时,进行一级缓存刷新.
     *
     * @param keys keys
     * @return void
     */
    default Mono<Void> refresh(Collection<String> keys) {
        return Mono.empty();
    }

    /**
     * 刷新全部缓存信息,通常用于在二级缓存时,进行一级缓存刷新.
     *
     * @return void
     */
    default Mono<Void> refresh() {
        return Mono.empty();
    }
}
