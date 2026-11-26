package org.jetlinks.core.rpc.transport;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.function.Supplier;
/** 每次订阅从 supplier 取得 owned 帧，取消/失败也须回收；响应帧在 onNext 转移，尚未交付的响应仍由传输回收。 */
public interface RpcConnection {
    Mono<RpcFrame> requestResponse(Supplier<RpcFrame> request);
    Flux<RpcFrame> requestStream(Supplier<RpcFrame> request);
    Flux<RpcFrame> requestChannel(Publisher<RpcFrame> requests);
    Mono<Void> fireAndForget(Supplier<RpcFrame> request);
    Mono<Void> onClose();
    Mono<Void> close();
}
