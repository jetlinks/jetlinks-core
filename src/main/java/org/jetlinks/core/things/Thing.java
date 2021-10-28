package org.jetlinks.core.things;

import org.jetlinks.core.Configurable;
import reactor.core.publisher.Mono;

public interface Thing extends Configurable {

    String getId();

    Mono<? extends ThingMetadata> getMetadata();

    Mono<Void> updateMetadata(String metadata);

    Mono<Void> updateMetadata(ThingMetadata metadata);
}
