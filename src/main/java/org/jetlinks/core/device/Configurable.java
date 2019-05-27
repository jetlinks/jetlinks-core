package org.jetlinks.core.device;

import org.jetlinks.core.metadata.ValueWrapper;

import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface Configurable {

    ValueWrapper get(String key);

    void put(String key, Object value);

    void putAll(Map<String, Object> conf);


    void remove(String key);

}
