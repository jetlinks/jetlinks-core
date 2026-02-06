package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import org.jetlinks.core.principal.Credential;
import org.jetlinks.core.principal.Identity;
import org.jetlinks.core.principal.Principal;

@AllArgsConstructor
class SimpleDevicePrincipal implements DevicePrincipal {
    private final DeviceOperator device;
    private final Principal principal;
    private final boolean authorized;

    @Override
    public boolean isAuthorized() {
        return authorized;
    }

    @Override
    public DeviceOperator getDevice() {
        return device;
    }

    @Override
    public Identity identity() {
        return principal == null ? null : principal.identity();
    }

    @Override
    public Credential credential() {
        return principal == null ? null : principal.credential();
    }
}
