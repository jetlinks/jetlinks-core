package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.codec.http.websocket.WebSocketSession;

@Getter
@AllArgsConstructor(staticName = "of")
public class WebsocketAuthenticationRequest implements AuthenticationRequest {

    private WebSocketSession socketSession;


    @Override
    public Transport getTransport() {
        return DefaultTransport.WebSocket;
    }
}
