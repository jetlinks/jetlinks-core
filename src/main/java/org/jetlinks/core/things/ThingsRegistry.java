package org.jetlinks.core.things;

import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 物注册中心,统一管理物的基础信息以及配置等信息
 *
 * @author zhouhao
 * @since 1.1.9
 */
public interface ThingsRegistry {

    /**
     * 根据物类型获取物实例
     *
     * @param thingType 物类型
     * @param thingId   物实例
     * @return 物实例
     */
    Mono<Thing> getThing(@Nonnull ThingType thingType,
                         @Nonnull String thingId);

    /**
     * 根据物类型获取物模版
     *
     * @param thingType  物类型
     * @param templateId 物模版ID
     * @return 物模版实例
     */
    Mono<ThingTemplate> getTemplate(@Nonnull ThingType thingType,
                                    @Nonnull String templateId);

    /**
     * 注册指定物类型的物实例到注册中心
     *
     * @param thingType 物类型
     * @param info      物实例信息
     * @return 物实例
     */
    Mono<Thing> register(@Nonnull ThingType thingType,
                         @Nonnull ThingInfo info);

    /**
     * 从注册中心注销指定物类型的物实例,注销后,通过{@link ThingsRegistry#getThing(ThingType, String)}将无法获取到物实例
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @return void
     */
    Mono<Void> unregisterThing(@Nonnull ThingType thingType,
                               @Nonnull String thingId);

    /**
     * 注册指定物类型的物模版到注册中心
     *
     * @param thingType    物类型
     * @param templateInfo 物模版信息
     * @return 物实例
     */
    Mono<ThingTemplate> register(@Nonnull ThingType thingType,
                                 @Nonnull ThingTemplateInfo templateInfo);

    /**
     * 从注册中心注销指定物类型的物实例,注销后,通过{@link ThingsRegistry#getTemplate(ThingType, String)}将无法获取到物实例
     *
     * @param thingType 物类型
     * @param thingId   物ID
     * @return void
     */
    Mono<Void> unregisterTemplate(@Nonnull ThingType thingType,
                                  @Nonnull String templateId);

}
