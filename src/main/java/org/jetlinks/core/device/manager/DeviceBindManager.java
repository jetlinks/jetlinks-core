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
     * @param type     类型
     * @param key      绑定key
     * @param deviceId 平台的设备ID
     * @return void
     */
    Mono<Void> bind(@Nonnull String type,
                    @Nonnull String key,
                    @Nonnull String deviceId);

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
     * 根据key获取设备ID
     *
     * @param type 类型
     * @param key  绑定key
     * @return deviceId
     */
    Mono<BindInfo> getBindInfo(@Nonnull String type,
                               @Nonnull String key);

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
