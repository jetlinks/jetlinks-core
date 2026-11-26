package org.jetlinks.core.rpc.transport;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
/** 单请求帧借用至返回 Publisher 终止；channel 输入逐帧转移所有权；输出帧转移给传输方。 */
public interface RpcHandler {
    Mono<RpcFrame> requestResponse(RpcFrame request);
    Flux<RpcFrame> requestStream(RpcFrame request);
    Flux<RpcFrame> requestChannel(Publisher<RpcFrame> requests);
    Mono<Void> fireAndForget(RpcFrame request);
}
