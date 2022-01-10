package org.jetlinks.core.config;

/**
 * 配置键值对
 *
 * @param <V> 值类型
 * @author zhouhao
 * @since 1.0
 */
public interface ConfigKeyValue<V> extends ConfigKey<V> {

    /**
     * 获取值
     *
     * @return 值
     */
    V getValue();

    /**
     * 判断值是否为null
     *
     * @return true 表示为null,false 表示不为null
     */
    default boolean isNull() {
        return null == getValue();
    }

    /**
     * 判断值是否不为null
     *
     * @return true 表示不为null,false 表示为null
     */
    default boolean isNotNull() {
        return null != getValue();
    }

}
