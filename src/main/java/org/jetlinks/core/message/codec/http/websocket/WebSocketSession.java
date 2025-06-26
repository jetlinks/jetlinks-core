package org.jetlinks.core.message.codec.http.websocket;

import io.netty.buffer.ByteBuf;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.http.Header;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.jetlinks.core.server.ClientConnection;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WebSocketSession extends ClientConnection {

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

    Map<Object, Object> getAttributes();

   <T> Optional<T> getAttribute(Object key);

    void setAttribute(Object key, Object value);

    Flux<WebSocketMessage> receive();

    Mono<Void> send(WebSocketMessage message);

    WebSocketMessage textMessage(String text);

    WebSocketMessage binaryMessage(ByteBuf message);

    WebSocketMessage pingMessage(ByteBuf message);

    WebSocketMessage pongMessage(ByteBuf message);

    @Override
    default InetSocketAddress address(){
        return getRemoteAddress().orElse(null);
    }

    @Override
    default Mono<Void> sendMessage(EncodedMessage message){
        if(message instanceof WebSocketMessage){
            return send((WebSocketMessage) message);
        }
        return send(binaryMessage(message.getPayload()));
    }

    @Override
    default Flux<EncodedMessage> receiveMessage(){
        return receive().cast(EncodedMessage.class);
    }

    @Override
    default void disconnect(){
        close().subscribe();
    }

    @Override
    default void onDisconnect(Runnable callback) {
        ClientConnection.super.onDisconnect(callback);
    }
}
