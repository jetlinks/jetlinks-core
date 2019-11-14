package org.jetlinks.core.message.codec;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@AllArgsConstructor
public enum DefaultTransport implements Transport {
    MQTT("MQTT"),
    MQTT_SSL("MQTT SSL"),
    UDP("UDP"),
    UDP_DTLS("UDP DTLS"),
    CoAP("CoAP"),
    CoAP_DTLS("CoAP DTLS"),
    TCP("TCP"),
    TCP_SSL("TCP SSL"),
    HTTP("HTTP"),
    HTTPS("HTTPS"),
    WebSocket("WebSocket"),
    WebSockets("WebSocket SSL");

    @Getter
    private String name;

    @Override
    public String getId() {
        return name();
    }


}
