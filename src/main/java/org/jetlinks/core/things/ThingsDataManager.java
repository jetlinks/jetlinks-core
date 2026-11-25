package org.jetlinks.core.things;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;

/**
 * 物实例数据管理器,用于管理物实例最近n条数据.
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface ThingsDataManager {

    /** 批量读取有界近期历史，后端定义保留数量；区间为 [from,to)。 */
    default Mono<List<ThingProperty>> getProperties(String thingType, String thingId,
                                                    Collection<String> properties, long from, long to) {
        return Flux.fromIterable(properties)
                   .concatMap(property -> getProperties(thingType, thingId, property, from, to))
                   .collectList()
                   .map(batches -> {
                       List<ThingProperty> result = new java.util.ArrayList<>();
                       for (List<ThingProperty> batch : batches) result.addAll(batch);
                       return result;
                   });
    }

    /**
     * 批量读取基准时间前的最新观测；缺失字段不返回，多字段之间不保证事务快照。
     * 后端应覆写此方法以避免逐字段网络往返。
     */
    default Mono<List<ThingProperty>> getLastProperties(String thingType, String thingId,
                                                        Collection<String> properties, long baseTime) {
        return Flux.fromIterable(properties)
                   .flatMap(property -> getLastProperty(thingType, thingId, property, baseTime))
                   .collectList();
    }

    /** 批量读取事件的最近观测；不承诺可靠事件重放。 */
    default Mono<List<ThingEvent>> getLastEvents(String thingType, String thingId,
                                                 Collection<String> events, long baseTime) {
        return Flux.fromIterable(events)
                   .flatMap(event -> getLastEvent(thingType, thingId, event, baseTime))
                   .collectList();
    }

    /** 批量读取标签的最近观测。 */
    default Mono<List<ThingTag>> getLastTags(String thingType, String thingId,
                                            Collection<String> tags, long baseTime) {
        return Flux.fromIterable(tags)
                   .flatMap(tag -> getLastTag(thingType, thingId, tag, baseTime))
                   .collectList();
    }


    /**
     * 获取基准时间前最新的属性
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @param property  属性
     * @param baseTime  基准时间
     * @return 属性
     */
    Mono<ThingProperty> getLastProperty(String thingType,
                                        String thingId,
                                        String property,
                                        long baseTime);

    /**
     * 获取第一次上报的属性
     *
     * @param thingId  物ID
     * @param property 属性ID
     * @return 属性
     */
    Mono<ThingProperty> getFirstProperty(String thingType,
                                         String thingId,
                                         String property);

    /**
     * 获取最后一次属性变更时间
     *
     * @param thingId 物ID
     * @return 时间戳
     */
    Mono<Long> getLastPropertyTime(String thingType,
                                   String thingId,
                                   long baseTime);

    /**
     * 获取第一次上报数据的时间
     *
     * @param thingId 物ID
     * @return 时间戳
     */
    Mono<Long> getFirstPropertyTime(String thingType,
                                    String thingId);

    /**
     * 获取指定属性在基准时间范围的全部缓存数据,缓存的数据量由具体的实现决定,通常不会返回全部的历史数据.
     *
     * @param thingType 类型
     * @param thingId   物ID
     * @param property  属性
     * @param from      基准起始时间
     * @param to        基准截止时间
     * @return 属性数据
     * @since 1.20
     */
    default Mono<List<ThingProperty>> getProperties(String thingType,
                                                    String thingId,
                                                    String property,
                                                    long from,
                                                    long to) {
        if (from >= to) return Mono.just(java.util.Collections.emptyList());
        return this
                .getLastProperty(thingType, thingId, property, to - 1)
                .filter(prop -> prop.getTimestamp() >= from)
                .expandDeep(prop -> prop.getTimestamp() == Long.MIN_VALUE ? Mono.empty() : this
                        .getLastProperty(thingType, thingId, property, prop.getTimestamp() - 1)
                        .filter(p -> p.getTimestamp() >= from))
                .collectList();
    }

    /**
     * 获取指定属性在基准时间前的全部缓存数据,缓存的数据量由具体的实现决定,通常不会返回全部的历史数据.
     *
     * @param thingType 类型
     * @param thingId   物ID
     * @param property  属性
     * @param baseTime  基准时间
     * @return 属性数据
     * @since 1.20
     */
    default Mono<List<ThingProperty>> getProperties(String thingType,
                                                    String thingId,
                                                    String property,
                                                    long baseTime) {
        return getProperties(thingType, thingId, property, 0, baseTime);
    }


    /**
     * 获取基准时间前最新的属性
     *
     * @param thingId  物ID
     * @param property 属性
     * @param baseTime 基准时间
     * @return 属性
     */
    default Mono<ThingProperty> getLastProperty(ThingType thingType,
                                                String thingId,
                                                String property,
                                                long baseTime) {
        return getLastProperty(thingType.getId(), thingId, property, baseTime);
    }

    /**
     * 获取第一次上报的属性
     *
     * @param thingId  物ID
     * @param property 属性ID
     * @return 属性
     */
    default Mono<ThingProperty> getFirstProperty(ThingType thingType,
                                                 String thingId,
                                                 String property) {
        return getFirstProperty(thingType.getId(), thingId, property);
    }

    /**
     * 获取最后一次属性变更时间
     *
     * @param thingId 物ID
     * @return 时间戳
     */
    default Mono<Long> getLastPropertyTime(ThingType thingType,
                                           String thingId,
                                           long baseTime) {
        return getLastPropertyTime(thingType.getId(), thingId, baseTime);
    }

    /**
     * 获取第一次上报数据的时间
     *
     * @param thingId 物ID
     * @return 时间戳
     */
    default Mono<Long> getFirstPropertyTime(ThingType thingType,
                                            String thingId) {
        return getFirstPropertyTime(thingType.getId(), thingId);
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
    default Mono<ThingEvent> getLastEvent(String thingType,
                                          String thingId,
                                          String event,
                                          long baseTime) {
        return Mono.empty();
    }

    /**
     * 获取基准时间前最新的标签数据,缓存的数据量由具体的实现决定,只能获取最近的n条数据.
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @param tag       物模型标签
     * @param baseTime  基准时间
     * @return 标签数据
     */
    default Mono<ThingTag> getLastTag(String thingType,
                                        String thingId,
                                        String tag,
                                        long baseTime) {
        return Mono.empty();
    }
}
