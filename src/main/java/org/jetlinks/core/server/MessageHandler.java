package org.jetlinks.core.server;

import org.jetlinks.core.cluster.ServerNode;
import org.jetlinks.core.device.DeviceStateInfo;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Message;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 消息处理器,在服务启动后,用于接收来着平台的指令并进行相应的处理
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageHandler {

    /**
     * 监听发往设备的指令
     *
     * @param serverId 服务ID,在集群时,不同的节点serverId不同 {@link ServerNode#getId()}
     * @return 发往设备的消息指令流
     */
    Flux<Message> handleSendToDeviceMessage(String serverId);

    /**
     * 监听发往设备的指令
     *
     * @param serverId 服务ID,在集群时,不同的节点serverId不同 {@link ServerNode#getId()}
     * @return 发往设备的消息指令流
     */
    default Disposable handleSendToDeviceMessage(String serverId, Function<Message, Mono<Void>> handler) {
        return handleSendToDeviceMessage(serverId)
            .flatMap(handler)
            .subscribe();
    }

    /**
     * 监听获取设备真实状态请求,并响应状态结果
     *
     * @param serverId    服务ID,在集群时,不同的节点serverId不同
     * @param stateMapper 状态检查器
     * @deprecated {@link  org.jetlinks.core.device.session.DeviceSessionManager#isAlive(String)}
     */
    @Deprecated
    Disposable handleGetDeviceState(String serverId, Function<Publisher<String>, Flux<DeviceStateInfo>> stateMapper);

    /**
     * 回复平台下发的指令
     *
     * @param message 回复指令
     * @return success
     */
    Mono<Boolean> reply(DeviceMessageReply message);

}
