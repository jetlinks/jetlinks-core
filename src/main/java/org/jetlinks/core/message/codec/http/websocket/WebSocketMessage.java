package org.jetlinks.core.message.codec.http.websocket;

import io.netty.buffer.ByteBufUtil;
import org.jetlinks.core.message.codec.EncodedMessage;

import java.nio.charset.StandardCharsets;

public interface WebSocketMessage extends EncodedMessage {

    Type getType();

    enum Type {

        TEXT,

        BINARY,

        PING,

        PONG
    }

    default String print() {
        StringBuilder builder = new StringBuilder();

        builder.append(getType().name())
                .append("\n\n");
        if (ByteBufUtil.isText(getPayload(), StandardCharsets.UTF_8)) {
            builder.append(payloadAsString());
        } else {
            builder.append(ByteBufUtil.hexDump(getPayload()));
        }
        return builder.toString();

    }
}
