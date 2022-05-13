package org.jetlinks.core.rpc;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RpcManager {

    <T> Disposable registerService(T api);

    <T> Disposable registerService(String service,T api);

    <API> Flux<API> getServices(Class<API> service);

    <API> Mono<API> getService(String serverNodeId,
                               Class<API> service);

   <API> Flux<ServiceEvent> listen(Class<API> service);


}
