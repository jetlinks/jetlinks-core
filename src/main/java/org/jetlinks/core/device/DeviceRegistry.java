package org.jetlinks.core.device;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 设备注册中心
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceRegistry {

    /**
     * 获取设备操作接口.
     *
     * @param deviceId 设备ID
     * @return 设备操作接口
     */
    Mono<DeviceOperator> getDevice(String deviceId);

    /**
     * 批量检查设备真实状态
     *
     * @param id ID
     * @return 设备状态信息流
     */
    default Flux<DeviceStateInfo> checkDeviceState(Flux<? extends Collection<String>> id) {
        return Flux.error(new UnsupportedOperationException());
    }

    /**
     * 获取设备产品操作,请勿缓存返回值,注册中心已经实现本地缓存.
     *
     * @param productId 产品ID
     * @return 设备操作接口
     */
    Mono<DeviceProductOperator> getProduct(String productId);

    /**
     * 注册设备,并返回设备操作接口,请勿缓存返回值,注册中心已经实现本地缓存.
     *
     * @param deviceInfo 设备基础信息
     * @return 设备操作接口
     * @see this#getDevice(String)
     */
    Mono<DeviceOperator> register(DeviceInfo deviceInfo);

    /**
     * 注册产品(型号)信息
     *
     * @param productInfo 产品(型号)信息
     * @return 注册结果
     */
    Mono<DeviceProductOperator> register(ProductInfo productInfo);

    /**
     * 注销设备
     *
     * @param deviceId 设备ID
     * @return void
     */
    Mono<Void> unregisterDevice(String deviceId);

    /**
     * 注销产品型号
     *
     * @param productId 产品型号ID
     * @return void
     */
    Mono<Void> unregisterProduct(String productId);


}
