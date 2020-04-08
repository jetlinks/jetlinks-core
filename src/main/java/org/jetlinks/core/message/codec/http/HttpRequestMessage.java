package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface HttpRequestMessage extends EncodedMessage {

    @Nonnull
    String getUrl();

    @Nonnull
    HttpMethod getMethod();

    @Nullable
    MediaType getContentType();

    @Nonnull
    List<Header> getHeaders();

    @Nullable
    Map<String, String> getQueryParameters();

    /**
     * @see org.springframework.http.MediaType#APPLICATION_FORM_URLENCODED_VALUE 专属
     */
    @Nullable
    Map<String, String> getRequestParam();

    default Optional<Header> getHeader(String name) {
        return getHeaders().stream()
                .filter(header -> header.getName().equals(name))
                .findFirst();
    }

    default Optional<String> getQueryParameter(String name) {
        return Optional.ofNullable(getQueryParameters())
                .map(map -> map.get(name));
    }

    default String print() {
        StringBuilder builder = new StringBuilder();
        builder.append(getMethod()).append(" ").append(getUrl());
        if (!CollectionUtils.isEmpty(getQueryParameters())) {
            builder.append("?").append(getQueryParameters().entrySet().stream()
                    .map(e -> e.getKey().concat("=").concat(e.getValue()))
                    .collect(Collectors.joining("&")))
                    .append("\n");
        } else {
            builder.append("\n");
        }
        for (Header header : getHeaders()) {
            builder
                    .append(header.getName()).append(": ").append(String.join(",", header.getValue()))
                    .append("\n");
        }
        ByteBuf payload = getPayload();
        if (payload.readableBytes() == 0) {
            return builder.toString();
        }
        builder.append("\n");
        if (ByteBufUtil.isText(payload, StandardCharsets.UTF_8)) {
            builder.append(payload.toString(StandardCharsets.UTF_8));
        } else {
            ByteBufUtil.appendPrettyHexDump(builder, payload);
        }
        return builder.toString();
    }

}
