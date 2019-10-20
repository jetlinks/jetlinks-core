package org.jetlinks.core.server.mqtt;

import org.jetlinks.core.server.session.DeviceSession;

public interface MqttClientAck {

    DeviceSession getSession();

    int getMessageId();

    AckType getAckType();

    void doAck();
}
