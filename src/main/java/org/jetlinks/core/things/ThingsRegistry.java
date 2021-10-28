package org.jetlinks.core.things;

import reactor.core.publisher.Mono;

public interface ThingsRegistry {

    Mono<Thing> getThing(String thingId);

    Mono<Thing> register(ThingInfo info);

    Mono<Void> unregister(String thingId);

}
