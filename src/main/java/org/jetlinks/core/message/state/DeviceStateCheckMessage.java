package org.jetlinks.core.message.state;

import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;

/**
 * 设备状态检查消息，通常用于子设备的状态检查
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
