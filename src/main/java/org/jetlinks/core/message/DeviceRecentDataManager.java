package org.jetlinks.core.message;

import reactor.core.publisher.Mono;

/**
 * 设备最近数据管理器,用于获取设备最新的相关数据
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface DeviceRecentDataManager {

    /**
     * 获取最近的属性数据
     *
     * @param deviceId   设备ID
     * @param propertyId 属性ID
     * @return 属性数据
     */
    Mono<Object> getRecentProperty(String deviceId, String propertyId);

}
