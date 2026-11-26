package org.jetlinks.core.rpc.transport;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.function.Supplier;
/** 仅负责帧和连接；acceptor 完成后才能启动依赖连接就绪的反向目录调用。 */
public interface RpcTransport {
    /** maxPayloadBytes 为本节点接收的 data+metadata 预算，provider 在重组前约束分片总量。 */
    Mono<RpcServer> bind(URI address, int maxPayloadBytes, Acceptor acceptor);
    Mono<RpcConnection> connect(URI address, int maxPayloadBytes, Supplier<RpcFrame> setup, RpcHandler inbound);
    /** 关闭监听、连接和本 transport 拥有的执行资源。 */
    Mono<Void> close();
    interface Acceptor {
        /** setup 借用至返回 Mono 终止；reverse 是同一连接的反向 requester。 */
        Mono<RpcHandler> accept(RpcFrame setup, RpcConnection reverse);
    }
}
