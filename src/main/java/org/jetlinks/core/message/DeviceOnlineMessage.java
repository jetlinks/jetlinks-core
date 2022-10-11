package org.jetlinks.core.message;

public class DeviceOnlineMessage extends CommonDeviceMessage<DeviceOnlineMessage> {
    public MessageType getMessageType() {
        return MessageType.ONLINE;
    }
}
