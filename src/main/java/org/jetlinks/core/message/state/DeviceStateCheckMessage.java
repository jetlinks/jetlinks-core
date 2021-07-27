package org.jetlinks.core.message.state;

import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.message.ChildDeviceMessage;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;

/**
 * 设备状态检查消息，通常用于子设备的状态检查。
 * <p>
 * 当子设备设置{@link org.jetlinks.core.device.DeviceConfigKey#selfManageState}为true时,
 * 在检查子设备状态时,将向网关发送{@link ChildDeviceMessage#getChildDeviceMessage()} 为{@link DeviceStateCheckMessage}的消息
 *
 * @author zhouhao
 * @since 1.1.6
 */
public class DeviceStateCheckMessage extends CommonDeviceMessage implements RepayableDeviceMessage<DeviceStateCheckMessageReply> {

    public static DeviceStateCheckMessage create(String deviceId) {
        DeviceStateCheckMessage message = new DeviceStateCheckMessage();
        message.setDeviceId(deviceId);
        message.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
        return message;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.STATE_CHECK;
    }

    @Override
    public DeviceStateCheckMessageReply newReply() {
        return new DeviceStateCheckMessageReply().from(this);
    }
}
