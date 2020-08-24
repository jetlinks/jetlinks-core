package org.jetlinks.core.config;

import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public interface ConfigStorage {

    Mono<Value> getConfig(String key);

    default Mono<Values> getConfigs(String... key) {
        return getConfigs(Arrays.asList(key));
    }

    Mono<Values> getConfigs(Collection<String> key);

    Mono<Boolean> setConfigs(Map<String, Object> values);

    Mono<Boolean> setConfig(String key, Object value);

    Mono<Boolean> remove(String key);

    Mono<Value> getAndRemove(String key);

    Mono<Boolean> remove(Collection<String> key);

    Mono<Boolean> clear();

}
