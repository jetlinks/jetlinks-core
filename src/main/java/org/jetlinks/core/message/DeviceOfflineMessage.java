package org.jetlinks.core.message;

public class DeviceOfflineMessage extends CommonDeviceMessage{
    public MessageType getMessageType() {
        return MessageType.OFFLINE;
    }
}
