package org.jetlinks.core.device.session;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceSession {
    String getId();

    String getDeviceId();

    DeviceOperator getOperation();

    long lastPingTime();

    long connectTime();

    void send(EncodedMessage encodedMessage);

    Transport getTransport();

    void close();

    void ping();

    boolean isAlive();
}
