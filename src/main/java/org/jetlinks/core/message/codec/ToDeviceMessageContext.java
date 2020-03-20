package org.jetlinks.core.message.codec;

import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

public interface ToDeviceMessageContext extends MessageEncodeContext {
    Mono<Boolean> sendToDevice(@Nonnull EncodedMessage message);

    Mono<Void> disconnect();

    @Nonnull
    DeviceSession getSession();
}
