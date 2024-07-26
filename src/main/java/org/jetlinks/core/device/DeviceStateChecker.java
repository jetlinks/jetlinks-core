package org.jetlinks.core.device;

import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

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
    @Nonnull
    Mono<Byte> checkState(@Nonnull DeviceOperator device);

    /**
     * 排序需要，值越小优先级越高
     *
     * @return 序号
     * @since 1.1.5
     */
    default long order() {
        return Long.MAX_VALUE;
    }
}
