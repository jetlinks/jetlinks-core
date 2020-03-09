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

    parentGatewayId("上级网关设备ID"),

    connectionServerId("当前设备连接的服务ID"),

    sessionId("设备会话ID");

    String name;

    public static ConfigKey<Boolean> isGatewayDevice = ConfigKey.of("isGatewayDevice", "是否为网关设备");

    @Override
    public String getKey() {
        return name();
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }
}
