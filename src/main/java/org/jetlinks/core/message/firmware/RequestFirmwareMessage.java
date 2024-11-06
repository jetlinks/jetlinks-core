package org.jetlinks.core.message.firmware;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 拉取固件更新请求
 *
 * @see RequestFirmwareMessageReply
 * @since 1.0.3
 */
@Getter
@Setter
public class RequestFirmwareMessage extends CommonDeviceMessage<RequestFirmwareMessage> implements RepayableDeviceMessage<RequestFirmwareMessageReply> {

    //当前设备固件版本,没有则不传
    private String currentVersion;

    //申请更新固件版本,为空则为最新版
    private String requestVersion;

    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_FIRMWARE;
    }

    @Override
    public MessageType getReplyType() {
        return MessageType.REQUEST_FIRMWARE_REPLY;
    }

    @Override
    public RequestFirmwareMessageReply newReply() {
        return new RequestFirmwareMessageReply().from(this);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.currentVersion = SerializeUtils.readNullableUTF(in);
        this.requestVersion = SerializeUtils.readNullableUTF(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializeUtils.writeNullableUTF(currentVersion, out);
        SerializeUtils.writeNullableUTF(requestVersion, out);

    }
}
