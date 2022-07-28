package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.metadata.Feature;

@AllArgsConstructor
@Getter
public enum DeviceFeatures implements Feature {

    //标识使用此协议的设备支持固件升级
    supportFirmware("支持固件升级");

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
