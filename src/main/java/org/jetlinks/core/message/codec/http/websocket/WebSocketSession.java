package org.jetlinks.core.message.codec.http.websocket;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.http.Header;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WebSocketSession {

    Optional<InetSocketAddress> getRemoteAddress();

    default String getPath() {
        return getUri();
    }

    String getUri();

    @Nonnull
    List<Header> getHeaders();

    Optional<Header> getHeader(String key);

    Mono<Void> close();

    Mono<Void> close(int status);

    Map<String, Object> getAttributes();

    Optional<Object> getAttribute(String key);

    void setAttribute(String key, Object value);

    Flux<WebSocketMessage> receive();

    Mono<Void> send(WebSocketMessage message);

    WebSocketMessage textMessage(String text);

    WebSocketMessage binaryMessage(ByteBuf message);

    WebSocketMessage pingMessage(ByteBuf message);

    WebSocketMessage pongMessage(ByteBuf message);

}
