package org.jetlinks.core.message;

import lombok.Getter;
import lombok.Setter;

/**
 * 设备注册消息,通常用于子设备连接,并自动与父设备进行绑定
 *
 * @author zhouhao
 * @see ChildDeviceMessage
 * @since 1.0
 */
@Getter
@Setter
public class DeviceRegisterMessage extends CommonDeviceMessage {

    @Override
    public MessageType getMessageType() {
        return MessageType.REGISTER;
    }
}
