package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;

/**
 * 设备注销消息,与{@link DeviceRegisterMessage}相反
 *
 * @author zhouhao
 * @see ChildDeviceMessage
 * @since 1.0
 */
@Getter
@Setter
public class DeviceUnRegisterMessage extends CommonDeviceMessage {
    @Override
    public MessageType getMessageType() {
        return MessageType.UN_REGISTER;
    }
}
