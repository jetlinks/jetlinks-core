package org.jetlinks.core.server;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.message.DeviceMessage;
import reactor.core.publisher.Mono;

/**
 * 设备网关上下文,通过上下文可进行设备相关操作
 *
 * @author zhouhao
 * @since  1.1.6
 */
public interface DeviceGatewayContext {

    /**
     * 根据ID获取设备操作接口
     *
     * @param deviceId 设备ID
     * @return 设备操作接口
     */
    Mono<DeviceOperator> getDevice(String deviceId);

    /**
     * 根据产品ID获取产品操作接口
     *
     * @param productId 产品ID
     * @return 产品操作接口
     */
    Mono<DeviceProductOperator> getProduct(String productId);

    /**
     * 发送设备消息到设备网关,由平台统一处理这个消息.
     *
     * @param message 设备消息
     * @return void
     */
    Mono<Void> onMessage(DeviceMessage message);

}
