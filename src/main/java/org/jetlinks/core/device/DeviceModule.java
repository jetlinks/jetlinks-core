package org.jetlinks.core.device;

import org.jetlinks.core.things.Thing;

/**
 * 设备模块实例物操作对象.
 * <p>
 * 模块定义来自父设备/产品物模型中的 {@code modules[id]}, 模块实例来自具体设备的模块管理数据。
 * 同一模块定义可在同一设备下创建多个模块实例。
 *
 * @author zhouhao
 * @since 2.3.0
 */
public interface DeviceModule extends Thing {

    /**
     * @return 父设备ID
     */
    String getDeviceId();

    /**
     * 模块定义编码,对应物模型 {@code modules[id]}.
     *
     * @return 模块定义编码
     */
    String getCode();

    /**
     * 模块实例编码,用于区分同一设备下同一模块定义的多个实例.
     *
     * @return 模块实例编码
     */
    String getInstanceCode();

}
