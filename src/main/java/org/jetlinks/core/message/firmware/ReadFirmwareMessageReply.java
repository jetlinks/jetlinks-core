package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

import java.util.Map;

/**
 * 读取固件信息回复
 *
 * @see ReadFirmwareMessage
 * @since 1.0.3
 */
@Getter
@Setter
public class ReadFirmwareMessageReply extends CommonDeviceMessageReply<ReadFirmwareMessageReply> {

    private Map<String, Object> firmwareInfo;

    @Override
    public MessageType getMessageType() {
        return MessageType.READ_FIRMWARE_REPLY;
    }

}
