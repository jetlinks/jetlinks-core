package org.jetlinks.core;

import org.jetlinks.core.metadata.ValueWrapper;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletionStage;

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
     * @see ValueWrapper#value()
     */
    Mono<ValueWrapper> get(String key);

    /**
     * 获取多个配置,如果未指定key,则获取全部配置
     *
     * @param key key
     * @return 所有配置结果集合
     */
    Mono<Map<String, Object>> getAll(String... key);

    /**
     * 异步获取全部配置
     *
     * @param key 配置key
     * @return value
     */
    @Deprecated
    CompletionStage<Map<String, Object>> getAllAsync(String... key);

    /**
     * 设置一个配置,配置最好以基本数据类型或者json为主
     *
     * @param key   配置key
     * @param value 值 不能为null
     */
    Mono<Void> put(String key, Object value);

    /**
     * 批量设置配置
     *
     * @param conf 配置内容
     */
    Mono<Void> putAll(Map<String, Object> conf);

    /**
     * 删除配置
     *
     * @param key key
     * @return 被删除的值，不存在则返回empty
     */
    Mono<Object> remove(String key);

}
