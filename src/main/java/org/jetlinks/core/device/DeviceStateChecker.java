package org.jetlinks.core.device;

import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

/**
 * 设备状态检查器,用于自定义设备状态检查
 *
 * @since 1.0.2
 */
public interface DeviceStateChecker {

    /**
     * 检查设备状态
     *
     * @param device 设备操作接口
     * @return 设备状态 {@link DeviceState}
     * @see DeviceState
     */
    @NotNull
    Mono<Byte> checkState(@NotNull DeviceOperator device);

}
