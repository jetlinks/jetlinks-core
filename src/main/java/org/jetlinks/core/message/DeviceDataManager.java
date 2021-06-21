package org.jetlinks.core.message;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

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
    Mono<PropertyValue> getLastProperty(@Nonnull String deviceId,
                                        @Nonnull String propertyId);

    /**
     * 获取基准时间前的最新数据
     *
     * @param deviceId   设备ID
     * @param propertyId 属性ID
     * @param baseTime   基准时间
     * @return 属性数据
     */
    Mono<PropertyValue> getLastProperty(@Nonnull String deviceId,
                                        @Nonnull String propertyId,
                                        long baseTime);

    /**
     * 获取第一次上报属性的数据
     *
     * @param deviceId   设备ID
     * @param propertyId 属性ID
     * @return 属性数据
     */
    Mono<PropertyValue> getFistProperty(@Nonnull String deviceId,
                                        @Nonnull String propertyId);

    /**
     * 获取基准时间前设备最后上报属性的时间,如果从未上报过则返回{@link Mono#empty()}
     *
     * @param deviceId 设备ID
     * @return 属性时间
     */
    Mono<Long> getLastPropertyTime(@Nonnull String deviceId, long baseTime);

    /**
     * 获取首次属性上报的时间,如果从未上报则返回{@link Mono#empty()}
     *
     * @param deviceId 设备ID
     * @return 属性时间
     */
    Mono<Long> getFirstPropertyTime(@Nonnull String deviceId);


    /**
     * 获取标签值,如果不传入tags参数或者参数为空,则获取全部标签
     *
     * @param deviceId  设备ID
     * @param tagIdList 标签ID
     * @return 标签信息
     */
    default Flux<TagValue> getTags(@Nonnull String deviceId, String... tagIdList) {
        return Flux.empty();
    }

    /**
     * 获取标签值并转为Map,key为标签ID,值为标签值
     *
     * @param deviceId  设备ID
     * @param tagIdList 标签ID
     * @return 标签信息
     */
    default Mono<Map<String, Object>> getTagMap(@Nonnull String deviceId, String... tagIdList) {
        return this
                .getTags(deviceId, tagIdList)
                .collectMap(TagValue::getTagId, TagValue::getValue);
    }

    interface TagValue {
        //标签标识
        String getTagId();

        //标签值
        Object getValue();
    }

    interface PropertyValue {
        //时间戳
        long getTimestamp();

        //值
        Object getValue();

        //状态
        default String getState() {
            return null;
        }
    }
}
