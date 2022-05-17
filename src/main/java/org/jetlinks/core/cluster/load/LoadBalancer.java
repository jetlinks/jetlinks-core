package org.jetlinks.core.cluster.load;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.math.MathFlux;

import java.util.Comparator;

public interface LoadBalancer {

    Disposable register(LoadSupplier loadSupplier);

    Flux<ServerLoad> loads();

    Flux<ServerLoad> loads(String serviceNodeId);

    Mono<ServerLoad> load(String serviceNodeId, String featureId);

    Flux<ServerLoad> localLoads();

    Mono<ServerLoad> localLoad(String featureId);

    default Mono<ServerLoad> minimumLoad(String featureId) {
        return MathFlux
                .min(loads(featureId),
                     Comparator.comparingLong(ServerLoad::getLoad));
    }

}
