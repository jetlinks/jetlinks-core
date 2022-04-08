package org.jetlinks.core.things.relation;

import org.jetlinks.core.config.ConfigKey;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface MemberPropertyOperation {

    Mono<Object> get(String key);

    <T> Mono<T> get(ConfigKey<T> key);

    Flux<MemberProperty> get(Collection<String> keys);

    Flux<MemberProperty> get(ConfigKey<?>... keys);
}
