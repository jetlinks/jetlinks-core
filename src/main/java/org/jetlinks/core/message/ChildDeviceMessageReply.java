package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.enums.ErrorCode;

import java.util.function.BiConsumer;

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
    public ChildDeviceMessageReply error(Throwable e) {
        doWithChildReply(e, DeviceMessageReply::error);
        return super.error(e);
    }

    @Override
    public ChildDeviceMessageReply error(ErrorCode errorCode) {
        doWithChildReply(errorCode, DeviceMessageReply::error);
        return super.error(errorCode);
    }

    @Override
    public ChildDeviceMessageReply message(String message) {
        doWithChildReply(message, DeviceMessageReply::message);
        return super.message(message);
    }

    @Override
    public ChildDeviceMessageReply code(String code) {
        doWithChildReply(code, DeviceMessageReply::code);
        return super.code(code);
    }

    public <T> void doWithChildReply(T arg, BiConsumer<DeviceMessageReply, T> childReplyConsumer) {
        if (childDeviceMessage instanceof DeviceMessageReply) {
            DeviceMessageReply child = ((DeviceMessageReply) childDeviceMessage);
            childReplyConsumer.accept(child, arg);
        }
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
