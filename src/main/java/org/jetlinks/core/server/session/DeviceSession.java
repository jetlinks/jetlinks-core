package org.jetlinks.core.server.session;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Mono;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceSession {
    String getId();

    String getDeviceId();

    DeviceOperator getOperator();

    long lastPingTime();

    long connectTime();

    Mono<Boolean> send(EncodedMessage encodedMessage);

    Transport getTransport();

    void close();

    void ping();

    boolean isAlive();

    void onClose(Runnable call);
}
