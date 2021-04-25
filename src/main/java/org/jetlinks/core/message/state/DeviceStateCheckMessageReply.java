package org.jetlinks.core.message.state;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.device.DeviceState;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

/**
 * 设备状态检查回复,如果设备存在则回复{@link DeviceStateCheckMessageReply#success(byte)}
 *
 * @author zhouhao
 * @since 1.1.6
 */
@Getter
@Setter
public class DeviceStateCheckMessageReply extends CommonDeviceMessageReply<DeviceStateCheckMessageReply> {

    /**
     * @see org.jetlinks.core.device.DeviceState
     */
    private byte state;

    public DeviceStateCheckMessageReply success(byte state) {
        this.state = state;
        return this;
    }

    public DeviceStateCheckMessageReply setOnline() {
        this.state = DeviceState.online;
        return this;
    }

    public DeviceStateCheckMessageReply setOffline() {
        this.state = DeviceState.offline;
        return this;
    }

    public DeviceStateCheckMessageReply setNoActive() {
        this.state = DeviceState.noActive;
        return this;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.STATE_CHECK_REPLY;
    }
}
