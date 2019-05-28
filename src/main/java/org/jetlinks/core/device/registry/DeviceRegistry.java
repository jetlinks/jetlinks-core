package org.jetlinks.core.device.registry;


import org.jetlinks.core.device.DeviceInfo;
import org.jetlinks.core.device.DeviceOperation;
import org.jetlinks.core.device.DeviceProductOperation;

/**
 * 设备注册中心
 *
 * @author zhouhao
 * @since 1.0.0
 */
public interface DeviceRegistry {

    /**
     * 获取设备操作接口
     *
     * @param deviceId 设备ID
     * @return 设备操作接口
     */
    DeviceOperation getDevice(String deviceId);

    /**
     * 获取设备产品操作
     *
     * @param productId 产品ID
     * @return 设备操作接口
     */
    DeviceProductOperation getProduct(String productId);

    /**
     * 注册设备,并返回设备操作接口
     *
     * @param deviceInfo 设备基础信息
     * @return 设备操作接口
     */
    DeviceOperation registry(DeviceInfo deviceInfo);

    /**
     * 注销设备
     *
     * @param deviceId 设备ID
     */
    void unRegistry(String deviceId);

}
