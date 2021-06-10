package org.jetlinks.core.message.codec;


import org.jetlinks.core.device.DeviceOperator;
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
public interface MessageCodecContext {

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
