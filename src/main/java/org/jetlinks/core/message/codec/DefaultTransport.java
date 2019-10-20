package org.jetlinks.core.message.codec;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public enum DefaultTransport implements Transport {
    MQTT,
    MQTTS,
    UDP,
    CoAP,
    TCP,
    HTTP;

    @Override
    public String getId() {
        return name();
    }
}
