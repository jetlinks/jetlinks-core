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
public class CommonDeviceMessage implements DeviceMessage {
    private static final long serialVersionUID = -6849794470754667710L;

    private String code;

    private String messageId;

    private String deviceId;

    private long timestamp = System.currentTimeMillis();

    public void error(ErrorCode errorCode) {
        code = errorCode.name();
        timestamp = System.currentTimeMillis();
    }

    public void from(DeviceMessage message) {
        this.messageId = message.getMessageId();
        this.deviceId = message.getDeviceId();
    }

    @Override
    public JSONObject toJson() {
        return (JSONObject) JSON.toJSON(this);
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        this.deviceId = jsonObject.getString("clientId");
        this.messageId = jsonObject.getString("messageId");
        this.code = jsonObject.getString("code");
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

}
