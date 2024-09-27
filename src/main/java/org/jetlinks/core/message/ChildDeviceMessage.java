package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

/**
 * 发往子设备的消息,通常是通过网关设备接入平台的设备.
 *
 * @author zhouhao
 * @see Message
 * @see ChildDeviceMessageReply
 * @see org.jetlinks.core.device.DeviceConfigKey#parentGatewayId
 * @since 1.0.0
 */
@Getter
@Setter
public class ChildDeviceMessage extends CommonDeviceMessage<ChildDeviceMessage> implements RepayableDeviceMessage<ChildDeviceMessageReply> {
    private String childDeviceId;

    private Message childDeviceMessage;

    public static ChildDeviceMessage create(String deviceId, DeviceMessage message) {
        ChildDeviceMessage msg = new ChildDeviceMessage();
        msg.setDeviceId(deviceId);
        msg.setMessageId(message.getMessageId());
        msg.setChildDeviceId(message.getDeviceId());
        msg.setChildDeviceMessage(message);
        return msg;
    }

    @Override
    public ChildDeviceMessageReply newReply() {
        ChildDeviceMessageReply reply = new ChildDeviceMessageReply();
        if (childDeviceMessage instanceof RepayableDeviceMessage) {
            reply.setChildDeviceMessage(((RepayableDeviceMessage<?>) childDeviceMessage).newReply());
        }
        reply.messageId(getMessageId());
        reply.deviceId(getDeviceId());
        reply.setChildDeviceId(getChildDeviceId());
        return reply;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        if (null != childDeviceMessage) {
            json.put("childDeviceMessage", childDeviceMessage.toJson());
        }
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(new JSONObject(Maps.filterKeys(jsonObject, k -> !"childDeviceMessage".equals(k))));

        JSONObject json = jsonObject.getJSONObject("childDeviceMessage");
        if (json != null) {
            childDeviceMessage = MessageType
                    .convertMessage(json)
                    .orElse(null);
        }
    }

    public MessageType getMessageType() {
        return MessageType.CHILD;
    }

    @Override
    public void validate() {
        if (childDeviceMessage instanceof ChildDeviceMessage) {
            Set<String> deviceId = new HashSet<>();
            Message msg = childDeviceMessage;
            do {
                String childId = ((ChildDeviceMessage) msg).getChildDeviceId();
                msg = ((ChildDeviceMessage) msg).getChildDeviceMessage();
                if (deviceId.contains(childId)) {
                    throw new DeviceOperationException(ErrorCode.CYCLIC_DEPENDENCE);
                }
                deviceId.add(childId);
            } while (msg instanceof ChildDeviceMessage);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializeUtils.writeNullableUTF(childDeviceId, out);
        out.writeObject(childDeviceMessage);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this.childDeviceId = SerializeUtils.readNullableUTF(in);
        this.childDeviceMessage = (Message) in.readObject();
    }

    @Override
    public MessageType getReplyType() {
        return MessageType.CHILD_REPLY;
    }
}
