package org.jetlinks.core.things;

import reactor.core.publisher.Mono;

public interface ThingMetadataCodec {

    Mono<? extends ThingMetadata> decode(String metadata);

   <T extends ThingMetadata> Mono<String> encode(T metadata);

}
