package org.jetlinks.core.rpc.transport;
import reactor.core.publisher.Mono;
import java.net.URI;
/** 已绑定的本地 RPC 监听器。 */
public interface RpcServer {
    URI address();
    Mono<Void> close();
}
