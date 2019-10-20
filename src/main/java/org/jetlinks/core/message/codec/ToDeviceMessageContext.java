package org.jetlinks.core.message.codec;

import reactor.core.publisher.Mono;

public interface ToDeviceMessageContext extends MessageEncodeContext {
    Mono<Boolean> sendToDevice(EncodedMessage message);

    Mono<Void> disconnect();
}
