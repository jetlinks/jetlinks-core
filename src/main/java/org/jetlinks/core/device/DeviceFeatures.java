package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.Feature;
import org.jetlinks.core.principal.Principal;

import java.util.Map;

@AllArgsConstructor
@Getter
public enum DeviceFeatures implements Feature {

    //标识使用此协议的设备支持固件升级
    supportFirmware("支持固件升级"),

    /**
     * 标识协议以身份表示+平台提供的认证信息获取设备信息
     *
     * @see DevicePrincipalManager
     * @see DeviceRegistry#resolveDevice(Principal)
     * @see org.jetlinks.core.ProtocolSupport#getDevicePrincipalMetadata(Transport, Map)
     */
    supportPrincipal("支持平台管理设备凭证");

    private final String name;

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String getType() {
        return "device-manage";
    }
}
