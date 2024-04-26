package org.jetlinks.core.things;

import lombok.AllArgsConstructor;
import org.jetlinks.core.message.ThingMessage;
import reactor.core.publisher.Flux;

import java.util.List;

@AllArgsConstructor
class CompositeThingRpcSupportChain implements ThingRpcSupportChain {

    final List<ThingRpcSupportChain> chains;

    @Override
    public Flux<? extends ThingMessage> call(ThingMessage message, ThingRpcSupport next) {
        int size = chains.size();

        if (size == 1) {
            return chains.get(0).call(message, next);
        }
        ThingRpcSupport temp = next;
        for (ThingRpcSupportChain chain : chains) {
            ThingRpcSupport ftemp = temp;
            temp = msg -> chain.call(msg, ftemp);
        }
        return temp.call(message);
    }
}
