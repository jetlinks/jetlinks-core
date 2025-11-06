package org.jetlinks.core.defaults;

import org.jetlinks.core.things.ThingMetadata;
import reactor.core.publisher.Mono;

public interface ThingMetadataManager {

    Mono<ThingMetadata> getThingMetadata(String thingId);

    Mono<ThingMetadata> getTemplateMetadata(String thingId);

}
