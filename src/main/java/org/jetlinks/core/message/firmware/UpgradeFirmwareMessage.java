package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.metadata.types.FileType;

import java.util.Map;

/**
 * 更新设备固件
 *
 * @since 1.0.3
 */
@Getter
@Setter
public class UpgradeFirmwareMessage extends CommonDeviceMessageReply<UpgradeFirmwareMessageReply> implements RepayableDeviceMessage<UpgradeFirmwareMessageReply> {

    /**
     * 固件类型: url,binary,base64
     */
    private FileType.BodyType type;

    /**
     * 固件内容
     */
    private Object body;

    /**
     * 更新参数
     */
    private Map<String, Object> parameters;

    @Override
    public UpgradeFirmwareMessageReply newReply() {
        return new UpgradeFirmwareMessageReply();
    }
}
