package org.jetlinks.core;

import org.jetlinks.core.metadata.ValueWrapper;

import java.util.Map;

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
     * @see java.util.Optional
     */
    ValueWrapper get(String key);

    /**
     * 获取多个配置,如果未指定key,则获取全部配置
     *
     * @param key key
     * @return 所有配置结果集合
     */
    Map<String, Object> getAll(String... key);

    /**
     * 设置一个配置,配置最好以基本数据类型或者json为主
     *
     * @param key   配置key
     * @param value 值 不能为null
     */
    void put(String key, Object value);

    /**
     * 批量设置配置
     *
     * @param conf 配置内容
     */
    void putAll(Map<String, Object> conf);

    /**
     * 删除配置
     *
     * @param key key
     * @return 被删除的值，不存在则返回null
     */
    Object remove(String key);

}
