package org.jetlinks.core.message;

import reactor.core.publisher.Mono;

/**
 * 设备最近数据管理器,用于获取设备最新的相关数据
 *
 * @author zhouhao
 * @since 1.1.6
 */
public interface DeviceDataManager {

    /**
     * 获取最后上报的属性数据
     *
     * @param deviceId   设备ID
     * @param propertyId 属性ID
     * @return 属性数据
     */
    Mono<PropertyValue> getLastProperty(String deviceId,
                                        String propertyId);

    /**
     * 获取基准时间前的最新数据
     *
     * @param deviceId   设备ID
     * @param propertyId 属性ID
     * @param baseTime   基准时间
     * @return 属性数据
     */
    Mono<PropertyValue> getLastProperty(String deviceId,
                                        String propertyId,
                                        long baseTime);

    /**
     * 获取第一次上报属性的数据
     *
     * @param deviceId   设备ID
     * @param propertyId 属性ID
     * @return 属性数据
     */
    Mono<PropertyValue> getFistProperty(String deviceId,
                                        String propertyId);

    /**
     * 获取基准时间前设备最后上报属性的时间,如果从未上报过则返回{@link Mono#empty()}
     *
     * @param deviceId 设备ID
     * @return 属性时间
     */
    Mono<Long> getLastPropertyTime(String deviceId,long baseTime);

    /**
     * 获取首次属性上报的时间,如果从未上报则返回{@link Mono#empty()}
     *
     * @param deviceId 设备ID
     * @return 属性时间
     */
    Mono<Long> getFirstPropertyTime(String deviceId);

    interface PropertyValue {
        //时间戳
        long getTimestamp();

        //值
        Object getValue();

        //状态
       default String getState(){
           return null;
       }
    }
}
