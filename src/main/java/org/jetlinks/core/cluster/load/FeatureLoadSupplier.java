package org.jetlinks.core.cluster.load;

import reactor.core.publisher.Mono;

public interface FeatureLoadSupplier {

    String featureId();

    Mono<Long> currentLoad();

    void init(LoadBalancer balancer);
}
