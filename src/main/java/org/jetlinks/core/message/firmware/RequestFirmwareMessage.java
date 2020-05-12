package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;

/**
 * 拉取固件更新请求
 *
 * @since 1.0.3
 * @see RequestFirmwareMessageReply
 */
@Getter
@Setter
public class RequestFirmwareMessage extends CommonDeviceMessage implements RepayableDeviceMessage<RequestFirmwareMessageReply> {

    //当前设备固件版本,没有则不传
    private String currentVersion;

    //申请更新固件版本,为空则为最新版
    private String requestVersion;

    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_FIRMWARE;
    }

    @Override
    public RequestFirmwareMessageReply newReply() {
        return new RequestFirmwareMessageReply().from(this);
    }
}
