package org.jetlinks.core.message.codec;


import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface FromDeviceMessageContext extends MessageDecodeContext {

    Mono<Void> sendToDevice(EncodedMessage message);

    Mono<Void> disconnect();
}
