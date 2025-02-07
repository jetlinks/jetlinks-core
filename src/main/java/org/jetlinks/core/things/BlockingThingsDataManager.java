package org.jetlinks.core.things;

import lombok.AllArgsConstructor;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * 支持阻塞获取物属性的数据管理器
 *
 * @author zhouhao
 * @see ThingsDataManager
 * @see org.jetlinks.core.device.DeviceThingType
 * @see BlockingThingsDataManager#getLastPropertyNow(String, String, String, long)
 * @see BlockingThingsDataManager#getLastPropertyNow(ThingType, String, String, long)
 * @since 1.2.3
 */
@AllArgsConstructor
public class BlockingThingsDataManager implements ThingsDataManager {

    private final ThingsDataManager target;
    private final Duration timeout;

    @Override
    public Mono<ThingProperty> getLastProperty(String thingType, String thingId, String property, long baseTime) {
        return target.getLastProperty(thingType, thingId, property, baseTime);
    }

    /**
     * 获取基准时间前最新的属性
     *
     * <pre>{@code
     *
     *  manager.getLastPropertyNow("device","设备ID","物模型属性ID",now)
     *
     * }</pre>
     *
     * @param thingType 物类型 如: device
     * @param thingId   物ID
     * @param property  属性
     * @param baseTime  基准时间
     * @return 属性
     */
    public ThingProperty getLastPropertyNow(String thingType, String thingId, String property, long baseTime) {
        return await(getLastProperty(thingType, thingId, property, baseTime));
    }

    /**
     * 获取第一次上报的属性
     *
     * @param thingId  物ID
     * @param property 属性ID
     * @return 属性
     */
    @Override
    public Mono<ThingProperty> getFirstProperty(String thingType, String thingId, String property) {
        return target.getFirstProperty(thingType, thingId, property);
    }

    public ThingProperty getFirstPropertyNow(String thingType, String thingId, String property) {
        return await(getFirstProperty(thingType, thingId, property));
    }

    @Override
    public Mono<Long> getLastPropertyTime(String thingType, String thingId, long baseTime) {
        return target.getLastPropertyTime(thingType, thingId, baseTime);
    }

    /**
     * 获取最后一次属性变更时间
     *
     * @param thingId 物ID
     * @return 时间戳
     */
    public Long getLastPropertyTimeNow(String thingType, String thingId, long baseTime) {
        return await(getLastPropertyTime(thingType, thingId, baseTime));
    }

    @Override
    public Mono<Long> getFirstPropertyTime(String thingType, String thingId) {
        return target.getFirstPropertyTime(thingType, thingId);
    }

    /**
     * 获取第一次上报数据的时间
     *
     * @param thingId 物ID
     * @return 时间戳
     */
    public Long getFirstPropertyTimeNow(String thingType, String thingId) {
        return await(getFirstPropertyTime(thingType, thingId));
    }

    @Override
    public Mono<List<ThingProperty>> getProperties(String thingType, String thingId, String property, long from, long to) {
        return target.getProperties(thingType, thingId, property, from, to);
    }

    /**
     * 获取指定属性在基准时间范围的全部缓存数据,缓存的数据量由具体的实现决定,通常不会返回全部的历史数据.
     *
     * @param thingType 类型
     * @param thingId   物ID
     * @param property  属性
     * @param from      基准起始时间
     * @param to        基准截止时间
     * @return 属性数据
     */
    public List<ThingProperty> getPropertiesNow(String thingType, String thingId, String property, long from, long to) {
        return await(getProperties(thingType, thingId, property, from, to));
    }

    @Override
    public Mono<List<ThingProperty>> getProperties(String thingType, String thingId, String property, long baseTime) {
        return target.getProperties(thingType, thingId, property, baseTime);
    }

    /**
     * 获取指定属性在基准时间前的全部缓存数据,缓存的数据量由具体的实现决定,通常不会返回全部的历史数据.
     *
     * @param thingType 类型
     * @param thingId   物ID
     * @param property  属性
     * @param baseTime  基准时间
     * @return 属性数据
     */
    public List<ThingProperty> getPropertiesNow(String thingType, String thingId, String property, long baseTime) {
        return await(getProperties(thingType, thingId, property, baseTime));
    }

    @Override
    public Mono<ThingProperty> getLastProperty(ThingType thingType, String thingId, String property, long baseTime) {
        return target.getLastProperty(thingType, thingId, property, baseTime);
    }

    /**
     * 获取基准时间前最新的属性
     *
     * @param thingId  物ID
     * @param property 属性
     * @param baseTime 基准时间
     * @return 属性
     */
    public ThingProperty getLastPropertyNow(ThingType thingType, String thingId, String property, long baseTime) {
        return await(getLastProperty(thingType, thingId, property, baseTime));
    }

    @Override
    public Mono<ThingProperty> getFirstProperty(ThingType thingType, String thingId, String property) {
        return target.getFirstProperty(thingType, thingId, property);
    }

    /**
     * 获取第一次上报的属性
     *
     * @param thingId  物ID
     * @param property 属性ID
     * @return 属性
     */
    public ThingProperty getFirstPropertyNow(ThingType thingType, String thingId, String property) {
        return await(getFirstProperty(thingType, thingId, property));
    }

    @Override
    public Mono<Long> getLastPropertyTime(ThingType thingType, String thingId, long baseTime) {
        return target.getLastPropertyTime(thingType, thingId, baseTime);
    }

    /**
     * 获取最后一次属性变更时间
     *
     * @param thingId 物ID
     * @return 时间戳
     */
    public Long getLastPropertyTimeNow(ThingType thingType, String thingId, long baseTime) {
        return await(getLastPropertyTime(thingType, thingId, baseTime));
    }

    @Override
    public Mono<Long> getFirstPropertyTime(ThingType thingType, String thingId) {
        return target.getFirstPropertyTime(thingType, thingId);
    }

    /**
     * 获取第一次上报数据的时间
     *
     * @param thingId 物ID
     * @return 时间戳
     */
    public Long getFirstPropertyTimeNow(ThingType thingType, String thingId) {
        return await(getFirstPropertyTime(thingType, thingId));
    }

    @Override
    public Mono<ThingEvent> getLastEvent(String thingType, String thingId, String event, long baseTime) {
        return target.getLastEvent(thingType, thingId, event, baseTime);
    }

    /**
     * 获取基准时间前最新的事件数据,缓存的数据量由具体的实现决定,只能获取最近的n条数据.
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @param event     物模型事件ID
     * @param baseTime  基准时间
     * @return 事件数据
     */
    public ThingEvent getLastEventNow(String thingType, String thingId, String event, long baseTime) {
        return await(getLastEvent(thingType, thingId, event, baseTime));
    }

    @Override
    public Mono<ThingTag> getLastTag(String thingType, String thingId, String tag, long baseTime) {
        return target.getLastTag(thingType, thingId, tag, baseTime);
    }

    /**
     * 获取基准时间前最新的标签数据.
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @param tag       物模型标签
     * @param baseTime  基准时间
     * @return 标签数据
     */
    public ThingTag getLastTagNow(String thingType, String thingId, String tag, long baseTime) {
        return await(getLastTag(thingType, thingId, tag, baseTime));
    }

    protected <T> T await(Mono<T> task) {
        return Reactors.await(task, timeout);
    }

}
