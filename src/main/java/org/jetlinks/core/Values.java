package org.jetlinks.core;

import org.jetlinks.core.config.ConfigKey;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 多个值包装器
 *
 * @author zhouhao
 * @since 1.0
 */
public interface Values {

    /**
     * 获取全部值
     *
     * @return 全部值
     */
    Map<String, Object> getAllValues();

    /**
     * 获取单个值
     *
     * @param key key
     * @return Optional包装的值
     */
    Optional<Value> getValue(String key);

    /**
     * 将当前的值与指定的值进行合并，并返回新的值
     *
     * @param source 要合并的源
     * @return 新的值
     */
    Values merge(Values source);

    /**
     * 值数量
     *
     * @return size
     */
    int size();

    /**
     * 获取指定key列表中不存在的key
     *
     * @param keys key
     * @return 不存在的key
     */
    Set<String> getNonExistentKeys(Collection<String> keys);

    default boolean isEmpty() {
        return size() == 0;
    }

    default boolean isNoEmpty() {
        return size() > 0;
    }

    default <T> Optional<T> getValue(ConfigKey<T> key) {
        return getValue(key.getKey())
                .map(val -> (val.as(key.getType())));
    }

    default String getString(String key, Supplier<String> defaultValue) {
        return getValue(key).map(Value::asString).orElseGet(defaultValue);
    }

    default String getString(String key, String defaultValue) {
        return getString(key, () -> defaultValue);
    }

    default Number getNumber(String key, Supplier<Number> defaultValue) {
        return getValue(key).map(Value::asNumber).orElseGet(defaultValue);
    }

    default Number getNumber(String key, Number defaultValue) {
        return getNumber(key, () -> defaultValue);
    }

    static Values of(Map<String, ?> values) {
        return SimpleValues.of((Map) values);
    }
}
