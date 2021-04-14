package org.jetlinks.core.server.mqtt;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum MqttConnectReturnCode {
    CONNECTION_REFUSED_IDENTIFIER_REJECTED((byte) 0x02),
    CONNECTION_REFUSED_SERVER_UNAVAILABLE((byte) 0x03),
    CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD((byte) 0x04),
    CONNECTION_REFUSED_NOT_AUTHORIZED((byte) 0x05);

    private final byte code;

    public byte getCode() {
        return code;
    }
}