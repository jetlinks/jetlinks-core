package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.device.identity.Identity;
import org.jetlinks.core.metadata.Feature;

@AllArgsConstructor
@Getter
public enum DeviceFeatures implements Feature {

    //标识使用此协议的设备支持固件升级
    supportFirmware("支持固件升级"),

    /**
     * 标识协议以身份表示+平台提供的认证信息获取设备信息
     * @see org.jetlinks.core.device.identity.DeviceIdentityManager
     * @see DeviceRegistry#getDevice(Identity)
     */
    supportIdentity("支持协议自定义设备身份信息");

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
