package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@AllArgsConstructor
class SimpleHttpExchangeMessage implements HttpExchangeMessage {

    public final HttpRequestMessage request;

    private final Function<HttpResponseMessage, Mono<Void>> responseHandler;

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        return request.getPayload();
    }

    @Nonnull
    @Override
    public Mono<Void> response(@Nonnull HttpResponseMessage message) {
        return responseHandler.apply(message);
    }

    @Nonnull
    public String getPath() {
        return request.getPath();
    }

    @Nonnull
    @Override
    public String getUrl() {
        return request.getUrl();
    }

    @Nonnull
    @Override
    public HttpMethod getMethod() {
        return request.getMethod();
    }

    @Nullable
    @Override
    public MediaType getContentType() {
        return request.getContentType();
    }

    @Nonnull
    @Override
    public List<Header> getHeaders() {
        return request.getHeaders();
    }

    @Nullable
    @Override
    public Map<String, String> getQueryParameters() {
        return request.getQueryParameters();
    }
}
