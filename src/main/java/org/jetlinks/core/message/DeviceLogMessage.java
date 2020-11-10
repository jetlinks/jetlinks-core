package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceLogMessage extends CommonDeviceMessage {

    private String log;

    @Override
    public MessageType getMessageType() {
        return MessageType.LOG;
    }
}
