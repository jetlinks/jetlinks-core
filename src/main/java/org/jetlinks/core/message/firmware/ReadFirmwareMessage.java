package org.jetlinks.core.message.firmware;

import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;

/**
 * 读取设备固件信息
 *
 * @since 1.0.3
 */
public class ReadFirmwareMessage extends CommonDeviceMessage<ReadFirmwareMessage> implements RepayableDeviceMessage<ReadFirmwareMessageReply> {

    @Override
    public ReadFirmwareMessageReply newReply() {
        return new ReadFirmwareMessageReply().from(this);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.READ_FIRMWARE;
    }

    @Override
    public MessageType getReplyType() {
        return MessageType.READ_FIRMWARE_REPLY;
    }
}
