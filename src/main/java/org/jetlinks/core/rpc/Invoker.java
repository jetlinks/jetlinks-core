package org.jetlinks.core.rpc;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Invoker<REQ, RES> extends Disposable {

    Flux<RES> invoke(Publisher<? extends REQ> payload);

    Flux<RES> invoke();

    default Flux<RES> invoke(REQ payload) {
        return invoke(Mono.just(payload));
    }


}
