package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;

import java.util.Map;

/**
 * 设备上报固件信息
 *
 * @since 1.0.3
 */
@Getter
@Setter
public class ReportFirmwareMessage extends CommonDeviceMessage {

    private Map<String, Object> firmwareInfo;

    @Override
    public MessageType getMessageType() {
        return MessageType.READ_FIRMWARE_REPLY;
    }
}
