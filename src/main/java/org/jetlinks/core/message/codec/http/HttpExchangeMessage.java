package org.jetlinks.core.message.codec.http;

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

}
