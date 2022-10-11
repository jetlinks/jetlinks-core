package org.jetlinks.core.message;

public class DeviceOfflineMessage extends CommonDeviceMessage<DeviceOfflineMessage>{
    public MessageType getMessageType() {
        return MessageType.OFFLINE;
    }
}
