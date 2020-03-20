package org.jetlinks.core.message.codec;

import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

public interface ToDeviceMessageContext extends MessageEncodeContext {
    Mono<Boolean> sendToDevice(EncodedMessage message);

    Mono<Void> disconnect();

    DeviceSession getSession();
}
