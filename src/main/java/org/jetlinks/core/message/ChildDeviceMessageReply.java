package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
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
public class ChildDeviceMessageReply extends CommonDeviceMessageReply<ChildDeviceMessageReply> {
    private String childDeviceId;

    private Message childDeviceMessage;

    public MessageType getMessageType() {
        return MessageType.CHILD_REPLY;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = super.toJson();
        if (null != childDeviceMessage) {
            json.put("childDeviceMessage", childDeviceMessage.toJson());
        }
        return json;
    }
}
