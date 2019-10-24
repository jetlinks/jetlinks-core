package org.jetlinks.core.cluster;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public interface ClusterNotifier {
    Mono<Boolean> sendNotify(String serverNodeId, String address, Publisher<?> payload);

    <T> Mono<T> sendNotifyAndReceive(String serverNodeId, String address, Mono<?> payload);

    <T> Flux<T> handleNotify(String address);

    <T,R> void handleNotify(String address, Function<T, Mono<R>> replyHandler);

}
