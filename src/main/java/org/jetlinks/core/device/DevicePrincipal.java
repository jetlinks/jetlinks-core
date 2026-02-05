package org.jetlinks.core.device;

import org.jetlinks.core.principal.Principal;

public interface DevicePrincipal extends Principal {

    DeviceOperator getDevice();

    static DevicePrincipal create(DeviceOperator device, Principal principal){
        return new SimpleDevicePrincipal(device,principal);
    }
}
