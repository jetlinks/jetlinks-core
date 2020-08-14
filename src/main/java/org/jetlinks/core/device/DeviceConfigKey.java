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

    sessionId("设备会话ID"),

    shadow("设备影子"),

    //遗言，用于缓存消息，等设备上线时发送指令
    will("遗言");

    String name;

    public static ConfigKey<Boolean> isGatewayDevice = ConfigKey.of("isGatewayDevice", "是否为网关设备");

    //通常用于子设备状态
    public static ConfigKey<Boolean> selfManageState = ConfigKey.of("selfManageState", "状态自管理");

    @Override
    public String getKey() {
        return name();
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }
}
