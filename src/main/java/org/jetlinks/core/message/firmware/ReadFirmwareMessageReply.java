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
 * @author zhouhao
 */
@Getter
@Setter
public class ReadFirmwareMessageReply extends CommonDeviceMessageReply<ReadFirmwareMessageReply> {

    //固件版本号
    private String version;

    //其他信息
    private Map<String, Object> properties;

    @Override
    public MessageType getMessageType() {
        return MessageType.READ_FIRMWARE_REPLY;
    }

}
