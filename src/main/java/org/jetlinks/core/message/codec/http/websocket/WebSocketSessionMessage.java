package org.jetlinks.core.message.codec.http.websocket;

public interface WebSocketSessionMessage extends WebSocketMessage {

    WebSocketSession getWebSocketSession();

}
