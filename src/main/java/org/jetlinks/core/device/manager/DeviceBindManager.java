package org.jetlinks.core.device.manager;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * 设备绑定管理器，通常用于将设备ID与第三方平台进行绑定等操作
 *
 * @author zhouhao
 * @since 1.1.3
 */
public interface DeviceBindManager {

    /**
     * 绑定设备,类型与key组合成唯一键
     *
     * @param type     类型 {@link DeviceBindProvider#getId()}
     * @param key      绑定key
     * @param deviceId 平台的设备ID
     * @return void
     */
    default Mono<Void> bind(@Nonnull String type,
                            @Nonnull String key,
                            @Nonnull String deviceId) {
        return bind(type, key, deviceId, null);
    }

    /**
     * 绑定设备,类型与key组合成唯一键
     *
     * @param type        类型 {@link DeviceBindProvider#getId()}
     * @param key         绑定key
     * @param deviceId    平台的设备ID
     * @param description 说明
     * @return void
     */
    Mono<Void> bind(@Nonnull String type,
                    @Nonnull String key,
                    @Nonnull String deviceId,
                    String description);

    /**
     * 批量绑定设备
     *
     * @param type      类型
     * @param bindInfos 绑定信息
     * @return void
     * @since 1.1.4
     */
    default Mono<Void> bindBatch(@Nonnull String type, Collection<BindInfo> bindInfos) {
        return Flux
                .fromIterable(bindInfos)
                .flatMap(bindInfo -> bind(type, bindInfo.getKey(), bindInfo.getDeviceId(), bindInfo.getDescription()))
                .then();
    }

    /**
     * 解绑设备
     *
     * @param type 类型
     * @param key  绑定key
     * @return void
     */
    Mono<Void> unbind(@Nonnull String type,
                      @Nonnull String key);

    /**
     * 按设备id解绑
     *
     * @param type     类型
     * @param deviceId 设备ID
     * @return void
     */
    Mono<Void> unbindByDevice(@Nonnull String type,
                              @Nonnull Collection<String> deviceId);

    /**
     * 根据key获取设备ID
     *
     * @param type 类型
     * @param key  绑定key
     * @return deviceId
     */
    Mono<BindInfo> getBindInfo(@Nonnull String type,
                               @Nonnull String key);

    /**
     * 根据deviceId获取绑定信息
     *
     * @param type     类型
     * @param deviceId deviceId
     * @return deviceId
     */
    default Mono<BindInfo> getBindInfoByDeviceId(@Nonnull String type,
                                                 @Nonnull String deviceId) {
        return Mono.error(new UnsupportedOperationException());
    }

    /**
     * 根据deviceId获取绑定信息
     *
     * @param type     类型
     * @param deviceId deviceId
     * @return deviceId
     */
    default Flux<BindInfo> getBindInfoByDeviceId(@Nonnull String type,
                                                 @Nonnull Collection<String> deviceId) {
        return Flux.error(new UnsupportedOperationException());
    }


    /**
     * 获取指定key对应的绑定信息
     *
     * @param type 类型
     * @return 绑定信息
     */
    Flux<BindInfo> getBindInfo(@Nonnull String type,
                               @Nonnull Collection<String> keys);

    /**
     * 获取类型下所有的绑定信息
     *
     * @param type 类型
     * @return 绑定信息s
     */
    Flux<BindInfo> getBindInfo(@Nonnull String type);


}
