package org.jetlinks.core.things.relation;

import org.jetlinks.core.config.ConfigKey;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 属性操作接口,用于获取对象相关属性.
 *
 * @author zhouhao
 * @see ObjectProperty
 * @since 1.2
 */
public interface PropertyOperation {

    Mono<Object> get(String key);

    default <T> Mono<T> get(ConfigKey<T> key) {
        return this
                .get(key.getKey())
                .map(key::convertValue);
    }

    default Flux<ObjectProperty> get(Collection<String> keys) {
        return Flux
                .fromIterable(keys)
                .flatMap(key -> get(key)
                        .map(o -> ObjectProperty.of(key, o)));
    }

    default Flux<ObjectProperty> get(ConfigKey<?>... keys) {
        return Flux
                .fromArray(keys)
                .flatMap(key -> this
                        .get(key)
                        .map(o -> ObjectProperty.of(key.getKey(), o)));
    }
}
