package org.jetlinks.core.device.manager;

/**
 * 设备绑定提供者,通常用于标识绑定的是哪个第三方平台
 *
 * @author zhouhao
 * @since 1.1.4
 */
public interface DeviceBindProvider {
    /**
     * 绑定标识,通常就是{@link BindInfo#getKey()}
     *
     * @return 绑定标识
     */
    String getId();

    /**
     * 绑定名称
     *
     * @return 绑定名称
     */
    String getName();
}
