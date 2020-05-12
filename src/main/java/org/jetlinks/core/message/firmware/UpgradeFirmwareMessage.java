package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.util.Map;

/**
 * 更新设备固件消息,平台->设备
 *
 * @since 1.0.3
 * @see UpgradeFirmwareMessageReply
 */
@Getter
@Setter
public class UpgradeFirmwareMessage extends CommonDeviceMessage implements RepayableDeviceMessage<UpgradeFirmwareMessageReply> {

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

    /**
     * 签名
     */
    private String sign;

    /**
     * 签名方式,md5,sha256
     */
    private String signMethod;

    @Override
    public UpgradeFirmwareMessageReply newReply() {
        return new UpgradeFirmwareMessageReply().from(this);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.UPGRADE_FIRMWARE;
    }
}
