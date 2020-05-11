package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.util.Map;

/**
 * 更新设备固件
 *
 * @since 1.0.3
 * @see UpgradeFirmwareMessageReply
 */
@Getter
@Setter
public class UpgradeFirmwareMessage extends CommonDeviceMessageReply<UpgradeFirmwareMessageReply> implements RepayableDeviceMessage<UpgradeFirmwareMessageReply> {

    /**
     * 固件下载地址
     */
    private String url;

    /**
     * 固件版本
     */
    private String version;

    /**
     * 其他参数
     */
    private Map<String, Object> parameters;

    @Override
    public UpgradeFirmwareMessageReply newReply() {
        return new UpgradeFirmwareMessageReply();
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.UPGRADE_FIRMWARE;
    }
}
