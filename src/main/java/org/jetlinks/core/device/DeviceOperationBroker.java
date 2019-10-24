package org.jetlinks.core.device;

import org.jetlinks.core.message.BroadcastMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;

public interface DeviceOperationBroker {

    Flux<DeviceStateInfo> getDeviceState(String deviceGatewayServerId, Collection<String> deviceIdList);

    Flux<DeviceMessageReply> handleReply(String messageId, Duration timeout);

    Mono<Integer> send(String deviceGatewayServerId, Publisher<? extends Message> message);

    Mono<Integer> send(Publisher<? extends BroadcastMessage> message);

}
