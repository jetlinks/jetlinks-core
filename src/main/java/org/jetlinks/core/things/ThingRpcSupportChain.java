package org.jetlinks.core.things;

import org.jetlinks.core.message.ThingMessage;
import reactor.core.publisher.Flux;

public interface ThingRpcSupportChain {

    Flux<? extends ThingMessage> call(ThingMessage message, ThingRpcSupport next);

}
