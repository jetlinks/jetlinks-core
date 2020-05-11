package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

/**
 * 拉取固件信息响应
 *
 * @since 1.0.3
 * @author zhouhao
 */
@Getter
@Setter
public class RequestFirmwareMessageReply extends CommonDeviceMessageReply<RequestFirmwareMessageReply> {

    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_FIRMWARE_REPLY;
    }
}
