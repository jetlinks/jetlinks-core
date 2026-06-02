package org.jetlinks.core.device;

import org.jetlinks.core.things.Thing;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * 设备模块物提供器,用于为支持模块能力的设备运行时按模块编码创建模块 {@link Thing}.
 *
 * @author zhouhao
 * @since 2.3.0
 */
public interface DeviceModuleThingProvider {

    /**
     * 获取设备下指定模块定义对应的所有模块实例物操作对象.
     *
     * @param device 父设备操作对象
     * @param code   模块定义编码,对应物模型 {@code modules[id]}
     * @return 模块实例物操作对象; 如果当前 provider 不支持此模块,可返回 {@link Flux#empty()}
     */
    Flux<DeviceModule> getModuleThings(DeviceOperator device, String code);

    /**
     * 获取设备下指定模块实例物操作对象.
     *
     * @param device       父设备操作对象
     * @param code         模块定义编码,对应物模型 {@code modules[id]}
     * @param instanceCode 模块实例编码
     * @return 模块实例物操作对象; 如果当前 provider 不支持此模块实例,可返回 {@link Mono#empty()}
     */
    default Mono<DeviceModule> getModuleThing(DeviceOperator device, String code, String instanceCode) {
        return getModuleThings(device, code)
            .filter(module -> Objects.equals(module.getInstanceCode(), instanceCode))
            .singleOrEmpty();
    }

}
