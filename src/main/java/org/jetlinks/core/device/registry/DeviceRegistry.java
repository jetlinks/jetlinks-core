package org.jetlinks.core.device.registry;


import org.jetlinks.core.device.DeviceInfo;
import org.jetlinks.core.device.DeviceOperation;
import org.jetlinks.core.device.DeviceProductOperation;
import reactor.core.publisher.Mono;

/**
 * 设备注册中心
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceRegistry {

    /**
     * 获取设备操作接口,请勿缓存返回值,注册中心已经实现本地缓存.
     *
     * @param deviceId 设备ID
     * @return 设备操作接口
     */
    Mono<DeviceOperation> getDevice(String deviceId);

    /**
     * 获取设备产品操作,请勿缓存返回值,注册中心已经实现本地缓存.
     *
     * @param productId 产品ID
     * @return 设备操作接口
     */
    Mono<DeviceProductOperation> getProduct(String productId);

    /**
     * 注册设备,并返回设备操作接口,请勿缓存返回值,注册中心已经实现本地缓存.
     *
     * @param deviceInfo 设备基础信息
     * @return 设备操作接口
     * @see this#getDevice(String)
     */
    Mono<DeviceOperation> registry(DeviceInfo deviceInfo);

    /**
     * 注销设备
     *
     * @param deviceId 设备ID
     */
    Mono<Void> unRegistry(String deviceId);

}
