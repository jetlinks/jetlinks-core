package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

/**
 * 透传设备消息
 *
 * @author zhouhao
 * @since 1.0.2
 */
@Getter
@Setter
public class DirectDeviceMessage extends CommonDeviceMessage {

    @Nonnull
    private byte[] payload;

    @Override
    public MessageType getMessageType() {
        return MessageType.DIRECT;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        setPayload(jsonObject.getBytes("payload"));
        setDeviceId(jsonObject.getString("deviceId"));
        setMessageId(jsonObject.getString("messageId"));
        Long ts = jsonObject.getLong("timestamp");
        if (null != ts) {
            setTimestamp(ts);
        }
    }
}
