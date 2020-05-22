package org.jetlinks.core.message.codec.http.websocket;

import org.jetlinks.core.message.codec.EncodedMessage;

public interface WebSocketMessage extends EncodedMessage {

    Type getType();

    enum Type {

        TEXT,

        BINARY,

        PING,

        PONG
    }
}
