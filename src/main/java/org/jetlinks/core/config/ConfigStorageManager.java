package org.jetlinks.core.config;

import reactor.core.publisher.Mono;

public interface ConfigStorageManager {

    Mono<ConfigStorage> getStorage(String id);

}
