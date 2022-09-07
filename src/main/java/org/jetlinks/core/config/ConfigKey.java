package org.jetlinks.core.config;

import org.jetlinks.core.utils.ConverterUtils;

import java.lang.reflect.Type;

/**
 * 用于定义配置key,增加key的可读性
 *
 * @param <V> 值类型
 * @author zhouhao
 * @since 1.0
 */
public interface ConfigKey<V> {
    /**
     * @return key
     */
    String getKey();

    /**
     * key的名称,说明
     *
     * @return name
     */
    default String getName() {
        return getKey();
    }

    /**
     * key 对应值的类型
     *
     * @return 类型
     */
    @Deprecated
    default Class<V> getType() {
        return (Class<V>) Object.class;
    }
    /**
     * key 对应值的类型
     *
     * @return 类型
     */
    default Type getValueType() {
        return Object.class;
    }

    default V convertValue(Object value) {
        return ConverterUtils.convert(value, getValueType());
    }

    /**
     * 根据一个字符串来创建一个ConfigKey,它的key和name的值都为此字符串
     *
     * @param key 字符串
     * @param <T> 值类型
     * @return ConfigKey
     */
    static <T> ConfigKey<T> of(String key) {
        return of(key, key);
    }

    /**
     * 指定key和名称创建key
     *
     * @param key  key
     * @param name 名称
     * @param <T>  值类型
     * @return ConfigKey
     */
    static <T> ConfigKey<T> of(String key, String name) {
        return SimpleConfigKey.of(key, name, Object.class);
    }

    /**
     * 指定key字符串和名称以及值类型创建key
     *
     * @param key  key
     * @param name 名称
     * @param <T>  值类型
     * @param type 类型
     * @return ConfigKey
     */
    static <T> ConfigKey<T> of(String key, String name, Type type) {
        return SimpleConfigKey.of(key, name, type);
    }

    /**
     * 使用指定的值，将key转为ConfigKeyValue
     *
     * @param value 值
     * @return ConfigKeyValue
     */
    default ConfigKeyValue<V> value(V value) {
        return new ConfigKeyValue<V>() {
            @Override
            public V getValue() {
                return value;
            }

            @Override
            public String getKey() {
                return ConfigKey.this.getKey();
            }

            @Override
            public String getName() {
                return ConfigKey.this.getName();
            }

            @Override
            public Type getValueType() {
                return ConfigKey.this.getValueType();
            }
        };
    }
}
