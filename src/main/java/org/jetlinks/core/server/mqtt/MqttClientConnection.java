package org.jetlinks.core.server.mqtt;

import reactor.core.publisher.Mono;

public interface MqttClientConnection {

    String getClientId();

    String getUsername();

    char[] getPassword();

    Mono<Boolean> close(byte code);

    Mono<Boolean> accept(String deviceId);
}
