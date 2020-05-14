package org.jetlinks.core.message.codec.http;

import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;


/**
 * 可响应的http消息
 *
 * @author zhouhao
 * @see HttpRequestMessage
 * @see HttpResponseMessage
 * @see SimpleHttpResponseMessage
 * @since 1.0.2
 */
public interface HttpExchangeMessage extends HttpRequestMessage {

    @Nonnull
    Mono<Void> response(@Nonnull HttpResponseMessage message);

    default Mono<Void> ok(@Nonnull String message) {
        return response(
                SimpleHttpResponseMessage.builder()
                        .contentType(MediaType.APPLICATION_JSON)
                        .status(200)
                        .body(message)
                        .build()
        );
    }

    default Mono<Void> error(int status, @Nonnull String message) {
        return response(SimpleHttpResponseMessage.builder()
                .contentType(MediaType.APPLICATION_JSON)
                .status(status)
                .body(message)
                .build());
    }

    default SimpleHttpResponseMessage.SimpleHttpResponseMessageBuilder newResponse() {
        return SimpleHttpResponseMessage.builder();
    }
}
