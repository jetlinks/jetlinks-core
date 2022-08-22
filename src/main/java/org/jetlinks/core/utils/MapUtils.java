package org.jetlinks.core.utils;

import com.google.common.collect.Maps;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class MapUtils {

    public static <K, V> Map<K, V> convertKeyValue(Map<?, ?> source,
                                                   Function<Object, K> keyMapper,
                                                   Function<Object, V> valueMapper) {
        if (source == null) {
            return null;
        }
        if (source.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<K, V> target = Maps.newLinkedHashMapWithExpectedSize(source.size());
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            target.put(keyMapper.apply(entry.getKey()), valueMapper.apply(entry.getValue()));
        }
        return target;
    }
}
