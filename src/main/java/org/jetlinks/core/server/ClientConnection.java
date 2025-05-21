package org.jetlinks.core.server;

import org.jetlinks.core.Attributes;
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

    /**
     * 监听连接断开,当连接断开时,将执行回调{@link Runnable#run()}
     *
     * @param callback callback
     * @since 1.2.2
     */
    default void onDisconnect(Runnable callback) {

    }

    default Attributes attributes() {
        return Attributes.empty();
    }
}
