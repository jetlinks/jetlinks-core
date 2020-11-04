package org.jetlinks.core.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DeviceConfigScope implements ConfigScope {

    product("产品"),
    device("设备");

    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
