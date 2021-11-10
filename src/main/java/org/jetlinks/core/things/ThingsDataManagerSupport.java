package org.jetlinks.core.things;

import org.jetlinks.core.metadata.PropertyMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 物数据管理支持,用于获取物的相关数据
 *
 * @author zhouhao
 * @since 1.1.7
 */
public interface ThingsDataManagerSupport {

    /**
     * 判断是否支持此物类型,只有支持的物类型,后续才会调用
     *
     * @param thingType 物类型
     * @return 是否支持
     */
    boolean isSupported(ThingType thingType);

    /**
     * 获取距离基准时间最近的任意一个属性信息
     *
     * @param thingType 物类型
     * @param thingId   物实例ID
     * @param baseTime  基准时间
     * @return 属性信息
     */
    Mono<ThingProperty> getAnyLastProperty(ThingType thingType,
                                           String thingId,
                                           long baseTime);

    /**
     * 获取指定属性距离基准时间最近的信息
     *
     * @param thingType 物类型
     * @param thingId   物实例ID
     * @param property  属性ID {@link PropertyMetadata#getId()}
     * @param baseTime  基准时间
     * @return 属性信息
     */
    Mono<ThingProperty> getLastProperty(ThingType thingType,
                                        String thingId,
                                        String property,
                                        long baseTime);

    /**
     * 获取指定属性首次上报的的信息
     *
     * @param thingType 物类型
     * @param thingId   物实例ID
     * @param property  属性ID {@link PropertyMetadata#getId()}
     * @return 属性信息
     */
    Mono<ThingProperty> getFirstProperty(ThingType thingType,
                                         String thingId,
                                         String property);

    /**
     * 获取物实例首次上报的任意属性信息
     *
     * @param thingType 物类型
     * @param thingId   物实例ID
     * @return 属性信息
     */
    Mono<ThingProperty> getFirstProperty(ThingType thingType, String thingId);

    /**
     * 订阅物的实时属性
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @return 实时属性信息流
     */
    Flux<ThingProperty> subscribeProperty(ThingType thingType,
                                          String thingId);
}
