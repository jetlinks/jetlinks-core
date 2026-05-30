package org.jetlinks.core.device;

import org.jetlinks.core.things.Thing;
import reactor.core.publisher.Mono;

/**
 * 设备模块物提供器,用于为支持模块能力的设备运行时按模块编码创建模块 {@link Thing}.
 *
 * @author zhouhao
 * @since 2.3.0
 */
@FunctionalInterface
public interface DeviceModuleThingProvider {

    /**
     * 获取设备下指定模块对应的物操作对象.
     *
     * @param device     父设备操作对象
     * @param moduleCode 模块编码
     * @return 模块物操作对象; 如果当前 provider 不支持此模块,可返回 {@link Mono#empty()}
     */
    Mono<Thing> getModuleThing(DeviceOperator device, String moduleCode);

}
