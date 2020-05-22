package org.jetlinks.core.message.codec.http.websocket;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DefaultWebSocketMessage implements WebSocketMessage {

    Type type;

    ByteBuf payload;
}
