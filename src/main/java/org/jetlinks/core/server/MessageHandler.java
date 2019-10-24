package org.jetlinks.core.server;

import org.jetlinks.core.device.DeviceStateInfo;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public interface MessageHandler {

    Flux<Message> handleSendToDeviceMessage(String serverId);

    void handleGetDeviceState(String serverId, Function<Publisher<String>, Flux<DeviceStateInfo>> stateMapper);

    Mono<Boolean> reply(DeviceMessageReply message);

}
