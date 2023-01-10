package org.jetlinks.core.message.codec.http.websocket;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WebSocketSession {

    Optional<InetSocketAddress> getRemoteAddress();

    default String getPath() {

        return HttpUtils.getUrlPath(getUri());
    }

    default Map<String, String> getQueryParameters() {
        String uri = getUri();
        int indexOf = uri.indexOf('?');
        if (indexOf < 0) {
            return Collections.emptyMap();
        }
        return HttpUtils.parseEncodedUrlParams(uri);
    }

    String getUri();

    @Nonnull
    List<Header> getHeaders();

    Optional<Header> getHeader(String key);

    Mono<Void> close();

    Mono<Void> close(int status);

    default Mono<Void> close(HttpStatus status) {
        return close(1014, status.getReasonPhrase());
    }

    default Mono<Void> close(int status, String reason) {
        return close(status);
    }

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
