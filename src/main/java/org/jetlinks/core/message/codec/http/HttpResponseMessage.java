package org.jetlinks.core.message.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * HTTP响应消息
 *
 * @author zhouhao
 * @see SimpleHttpResponseMessage
 * @since 1.0
 */
public interface HttpResponseMessage extends EncodedMessage {

    int getStatus();

    MediaType getContentType();

    @Nonnull
    List<Header> getHeaders();

    default Optional<Header> getHeader(String name) {
        return getHeaders().stream()
                .filter(header -> header.getName().equals(name))
                .findFirst();
    }

    default String print() {
        StringBuilder builder = new StringBuilder();

        builder.append("HTTP").append(" ").append(HttpStatus.resolve(getStatus())).append("\n");
        boolean hasContentType = false;
        for (Header header : getHeaders()) {
            if (HttpHeaders.CONTENT_TYPE.equals(header.getName())) {
                hasContentType = true;
            }
            builder
                    .append(header.getName()).append(": ").append(String.join(",", header.getValue()))
                    .append("\n");
        }
        if (!hasContentType && null != getContentType()) {
            builder.append("Content-Type: ").append(getContentType()).append("\n");
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
