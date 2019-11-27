package org.jetlinks.core.server.mqtt;

import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.GatewayServerContext;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Flux;

public interface MqttGatewayServerContext extends GatewayServerContext {

    /**
     * @return MQTT or MQTT_TLS
     * @see DefaultTransport#MQTT
     * @see DefaultTransport#MQTT_TLS
     */
    @Override
    Transport getTransport();

    Flux<MqttClientConnection> handleUnknownClient();

    Flux<MqttSubscription> onSubscribe();

    Flux<MqttUnsubscription> onUnsubscribe();

    Flux<DeviceSession> onAccept();

    Flux<MqttClientAck> onAck();
}
