package org.jetlinks.core.message.codec;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@AllArgsConstructor
public enum DefaultTransport implements Transport {
    MQTT("MQTT"),
    MQTT_TLS("MQTT TLS"),
    UDP("UDP"),
    UDP_DTLS("UDP DTLS"),
    CoAP("CoAP"),
    CoAP_DTLS("CoAP DTLS"),
    TCP("TCP"),
    TCP_TLS("TCP TLS"),
    HTTP("HTTP"),
    HTTPS("HTTPS"),
    WebSocket("WebSocket"),
    WebSockets("WebSocket TLS");

    static {
        Transports.register(Arrays.asList(DefaultTransport.values()));
    }

    @Getter
    private final String name;

    @Override
    public String getId() {
        return name();
    }


}
