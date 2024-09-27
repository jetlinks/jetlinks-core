package org.jetlinks.core.device;

import org.jetlinks.core.cluster.ServerNode;
import org.jetlinks.core.message.BroadcastMessage;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collection;

/**
 * 设备操作代理,用于管理集群间设备指令发送
 *
 * @author zhouhao
 * @since 1.0
 */
public interface DeviceOperationBroker {

    /**
     * 获取指定服务里设备状态
     *
     * @param deviceGatewayServerId 设备所在服务ID {@link ServerNode#getId()}
     * @param deviceIdList          设备列表
     * @return 设备状态
     * @see org.jetlinks.core.device.session.DeviceSessionManager#isAlive(String)
     */
    Flux<DeviceStateInfo> getDeviceState(@Nullable String deviceGatewayServerId, Collection<String> deviceIdList);

    /**
     * 根据消息ID监听响应
     *
     * @param deviceId  设备ID
     * @param messageId 消息ID
     * @param timeout   超时时间
     * @return 消息返回
     */
    Flux<DeviceMessageReply> handleReply(String deviceId, String messageId, Duration timeout);


    /**
     * 根据消息ID监听响应
     *
     * @param message 消息
     * @param timeout 超时时间
     * @return 消息返回
     */
    default Flux<DeviceMessageReply> handleReply(DeviceMessage message, Duration timeout) {
        return handleReply(message.getDeviceId(), message.getMessageId(), timeout);
    }

    /**
     * 发送设备消息到指定到服务
     *
     * @param deviceGatewayServerId 设备所在服务ID {@link ServerNode#getId()}
     * @return 有多少服务收到了此消息
     * @see DeviceOperator#getConnectionServerId()
     */
    Mono<Integer> send(String deviceGatewayServerId, Publisher<? extends Message> message);

    /**
     * 发送广播消息
     *
     * @param message 广播消息
     * @return 有多少服务收到了此消息
     */
    Mono<Integer> send(Publisher<? extends BroadcastMessage> message);

}
