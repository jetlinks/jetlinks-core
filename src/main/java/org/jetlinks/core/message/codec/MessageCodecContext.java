package org.jetlinks.core.message.codec;


import org.jetlinks.core.Wrapper;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DevicePrincipal;
import org.jetlinks.core.principal.Principal;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * 消息编解码上下文
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageCodecContext extends Wrapper {

    /**
     * 获取当前上下文中到设备操作接口,
     * 在tcp,http等场景下,此接口可能返回{@code null}
     *
     * @return DeviceOperator
     */
    @Nullable
    DeviceOperator getDevice();

    /**
     * 同{@link MessageCodecContext#getDevice()},只是返回结果是Mono,不会为null.
     *
     * @return Mono<DeviceOperator>
     * @since 1.1.2
     */
    default Mono<DeviceOperator> getDeviceAsync() {
        return Mono.justOrEmpty(getDevice());
    }

    /**
     * 获取指定设备的操作接口.
     * 如果设备不存在,则为{@link Mono#empty()},可以通过{@link Mono#switchIfEmpty(Mono)}进行处理.
     *
     * @param deviceId 设备ID
     * @return Mono<DeviceOperator>
     * @since 1.1.2
     */
    default Mono<DeviceOperator> getDevice(String deviceId) {
        return Mono.empty();
    }

    /**
     * 根据设备凭证解析设备信息,用于通过自定义认证逻辑等场景获取平台内部设备信息.
     * <p>
     * 注意!!! 此操作不会校验凭证,只会返回设备以及对应的凭证.需要自己在协议包中进行校验.
     *
     * @param principal 身份信息
     * @return 设备操作接口
     * @since 1.3.2
     * @see org.jetlinks.core.ProtocolSupport#getDevicePrincipalMetadata(Transport, Map)
     * @see org.jetlinks.core.device.DeviceFeatures#supportPrincipal
     *
     */
    default Mono<DevicePrincipal> resolveDevice(Principal principal) {
        return getDevice(principal.identity().getIdentifier())
            .map(device -> DevicePrincipal.create(device, null));
    }

    /**
     * 预留功能,获取配置信息
     *
     * @return 配置信息
     */
    default Map<String, Object> getConfiguration() {
        return Collections.emptyMap();
    }

    /**
     * 预留功能,获取配置信息
     *
     * @param key KEY
     * @return 配置信息
     */
    default Optional<Object> getConfig(String key) {
        return Optional
            .ofNullable(getConfiguration())
            .map(conf -> conf.get(key));
    }
}
