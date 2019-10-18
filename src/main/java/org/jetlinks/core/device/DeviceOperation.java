package org.jetlinks.core.device;


import org.jetlinks.core.Configurable;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.metadata.DeviceMetadata;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * 设备操作接口
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceOperation extends Configurable {

    /**
     * @return 设备ID
     */
    String getDeviceId();

    /**
     * @return 当前设备连接所在服务器ID，如果设备未上线{@link DeviceState#online}，则返回{@link null}
     */
    Mono<String> getServerId();

    /**
     * @return 当前设备连接会话ID
     */
    Mono<String> getSessionId();

    /**
     * @param state 状态
     * @see DeviceState#online
     */
    Mono<Void> putState(byte state);

    /**
     * @return 获取当前状态
     * @see DeviceState
     */
    Mono<Byte> getState();

    /**
     * 检查设备的真实状态
     *
     * @see org.jetlinks.core.device.registry.DeviceMessageHandler#handleDeviceCheck(String, Consumer)
     */
    Mono<Byte> checkState();

    /**
     * @return 上线时间
     */
    Mono<Long> getOnlineTime();

    /**
     * @return 离线时间
     */
    Mono<Long> getOfflineTime();

    /**
     * 设备上线
     *
     * @param serverId  设备所在服务ID
     * @param sessionId 会话ID
     */
    Mono<Void> online(String serverId, String sessionId);

    /**
     * @return 是否在线
     */
    default Mono<Boolean> isOnline() {
        return getState()
                .map(state -> state.equals(DeviceState.online))
                .defaultIfEmpty(false);
    }

    /**
     * 设置设备离线
     *
     * @see DeviceState#offline
     */
    Mono<Void> offline();

    /**
     * 断开设备连接
     *
     * @return 断开结果
     */
    Mono<Boolean> disconnect();

    /**
     * 进行授权
     *
     * @param request 授权请求
     * @return 授权结果
     * @see MqttAuthenticationRequest
     */
    Mono<AuthenticationResponse> authenticate(AuthenticationRequest request);

    /**
     * @return 获取设备的元数据
     */
    Mono<DeviceMetadata> getMetadata();

    /**
     * @return 获取此设备使用的协议支持
     */
    Mono<ProtocolSupport> getProtocol();

    /**
     * @return 消息发送器, 用于发送消息给设备
     */
    DeviceMessageSender messageSender();

    /**
     * @return 获取设备的基本信息
     */
    Mono<DeviceInfo> getDeviceInfo();

    /**
     * 更新设备基本信息
     *
     * @param deviceInfo 设备信息
     */
    Mono<Void> update(DeviceInfo deviceInfo);

    /**
     * 更新元数据
     *
     * @param metadata 元数据
     */
    Mono<Void> updateMetadata(String metadata);

}
