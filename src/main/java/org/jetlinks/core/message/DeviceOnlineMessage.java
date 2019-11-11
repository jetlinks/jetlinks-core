package org.jetlinks.core.message;

public class DeviceOnlineMessage extends CommonDeviceMessage {
    public MessageType getMessageType() {
        return MessageType.ONLINE;
    }
}
