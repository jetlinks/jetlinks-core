package org.jetlinks.core.things;

import org.jetlinks.core.message.ThingMessage;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.*;

public interface ThingRpcSupportChain extends Ordered {

    Flux<? extends ThingMessage> call(ThingMessage message, ThingRpcSupport next);


    default ThingRpcSupportChain andThen(ThingRpcSupportChain then) {
        return new CompositeThingRpcSupportChain(Arrays.asList(this, then));
    }

    default ThingRpcSupportChain composite(Collection<ThingRpcSupportChain> chains) {
        List<ThingRpcSupportChain> composite = new ArrayList<>(chains);

        if (this instanceof CompositeThingRpcSupportChain) {
            composite.addAll(((CompositeThingRpcSupportChain) this).chains);
        } else {
            composite.add(this);
        }

        composite.sort(Comparator.comparingInt(ThingRpcSupportChain::getOrder));

        return new CompositeThingRpcSupportChain(composite);
    }

    @Override
    default int getOrder() {
        return Integer.MAX_VALUE;
    }
}
