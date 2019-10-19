package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.config.ConfigKey;

@AllArgsConstructor
@Getter
public enum DeviceConfigKey implements ConfigKey<String> {
    id("ID"),

    metadata("元数据"),

    productId("产品ID"),

    protocol("消息协议"),

    gatewayDeviceId("网关设备ID"),

    connectionServerId("当前设备连接的设备ID"),

    sessionId("设备会话ID");

    String name;

    @Override
    public String getKey() {
        return name();
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }
}
