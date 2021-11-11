package org.jetlinks.core.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.things.ThingType;
import org.jetlinks.core.things.ThingTypes;
import org.jetlinks.core.things.TopicSupport;

@AllArgsConstructor
public enum DeviceThingType implements ThingType, TopicSupport {
    device("设备");

    static {
        for (DeviceThingType value : values()) {
            ThingTypes.register(value);
        }
    }
    @Getter
    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
