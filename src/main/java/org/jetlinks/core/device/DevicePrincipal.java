package org.jetlinks.core.device;

import org.jetlinks.core.principal.Principal;

/**
 * 设备接入身份凭证信息
 *
 * @author zhouhao
 * @since 1.3.2
 */
public interface DevicePrincipal extends Principal {

    /**
     * @return 设备操作接口
     */
    DeviceOperator getDevice();

    /**
     * @return 是否已经授权
     */
    boolean isAuthorized();

    static DevicePrincipal create(DeviceOperator device, Principal principal) {
        return new SimpleDevicePrincipal(device, principal, false);
    }

    static DevicePrincipal createAuthorized(DeviceOperator device, Principal principal) {
        return new SimpleDevicePrincipal(device, principal, true);
    }
}
