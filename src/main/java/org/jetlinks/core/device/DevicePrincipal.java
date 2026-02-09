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
     * @return 身份是否已经验证
     */
    boolean isVerified();

    static DevicePrincipal create(DeviceOperator device, Principal principal) {
        return new SimpleDevicePrincipal(device, principal, false);
    }

    static DevicePrincipal createAuthorized(DeviceOperator device, Principal principal) {
        return new SimpleDevicePrincipal(device, principal, true);
    }

    static DevicePrincipal create(DeviceOperator device, DevicePrincipal principal) {
        if (principal == null) {
            return new SimpleDevicePrincipal(device, null, false);
        }
        return new SimpleDevicePrincipal(device, principal, principal.isVerified());
    }
}
