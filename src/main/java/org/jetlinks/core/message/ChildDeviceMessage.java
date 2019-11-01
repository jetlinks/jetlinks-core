package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;

/**
 * 子设备消息
 *
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ChildDeviceMessage extends CommonDeviceMessage implements RepayableDeviceMessage<ChildDeviceMessageReply> {
    private String childDeviceId;

    private Message childDeviceMessage;

    @Override
    public ChildDeviceMessageReply newReply() {
        return new ChildDeviceMessageReply();
    }
}
