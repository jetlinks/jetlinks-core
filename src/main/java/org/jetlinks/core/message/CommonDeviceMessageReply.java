package org.jetlinks.core.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.jetlinks.core.enums.ErrorCode;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommonDeviceMessageReply implements DeviceMessageReply {
    private static final long serialVersionUID = -6849794470754667710L;

    private boolean success;

    private String code;

    private String message;

    private String messageId;

    private String deviceId;

    private long timestamp;

    @Override
    public DeviceMessageReply error(ErrorCode errorCode) {
        success = false;
        code = errorCode.name();
        message = errorCode.getText();
        timestamp = System.currentTimeMillis();
        return this;
    }

    @Override
    public DeviceMessageReply from(DeviceMessage message) {
        this.messageId = message.getMessageId();
        this.deviceId = message.getDeviceId();
        return this;
    }

    @Override
    public JSONObject toJson() {
        return (JSONObject) JSON.toJSON(this);
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        success = jsonObject.getBoolean("success");
        timestamp = jsonObject.getLong("timestamp");
        messageId = jsonObject.getString("messageId");
        deviceId = jsonObject.getString("deviceId");
        code = jsonObject.getString("code");
        message = jsonObject.getString("message");
    }

    @Override
    public DeviceMessageReply messageId(String messageId) {
        this.messageId=messageId;
        return this;
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

}
