package org.jetlinks.core.server;

import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 客户端连接
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface ClientConnection {

    /**
     * @return 客户端地址
     */
    InetSocketAddress address();

    /**
     * 发送消息给客户端
     *
     * @param message 消息
     * @return void
     */
    Mono<Void> sendMessage(EncodedMessage message);

    /**
     * 接收来自客户端消息
     *
     * @return 消息流
     */
    Flux<EncodedMessage> receiveMessage();

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 连接是否还存活
     */
    boolean isAlive();
}
