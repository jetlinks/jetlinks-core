package org.jetlinks.core.device;

import org.jetlinks.core.message.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 设备消息处理器,用于接收并处理来自其他服务发往设备的消息并回复.
 * <p>
 * 此接口通常由设备网关服务使用,用户基本上无需使用此接口.
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceMessageHandler {

    Flux<Message> handleDeviceMessage(String serverId);

    void handleGetDeviceState(String serverId, Function<Publisher<String>, Flux<DeviceStateInfo>> stateMapper);

    Flux<DeviceStateInfo> getDeviceState(String serviceId, Publisher<String> deviceIdList);

    Mono<Boolean> reply(DeviceMessageReply message);

    Flux<DeviceMessageReply> handleReply(String messageId, Duration timeout);

    Mono<Integer> send(String serverId, Publisher<? extends Message> message);

    Mono<Integer> send(Publisher<? extends BroadcastMessage> message);

}
