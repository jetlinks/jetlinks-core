package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.GenericHeaderSupport;
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
@SuppressWarnings("all")
public class CommonDeviceMessageReply<Self extends CommonDeviceMessageReply<Self>> extends GenericHeaderSupport<Self> implements DeviceMessageReply {
    private static final long serialVersionUID = -6849794470754667710L;

    private boolean success = true;

    private String code;

    private String message;

    private String messageId;

    private String deviceId;

    private long timestamp = System.currentTimeMillis();


    @Override
    @JsonIgnore
    @JSONField(serialize = false)
    public final String getThingId() {
        return getDeviceId();
    }

    @Override
    public final String getThingType() {
        return DeviceMessageReply.super.getThingType();
    }


    public Self code(String code) {
        this.code = code;

        return castSelf();
    }

    public Self message(String message) {
        this.message = message;

        return castSelf();
    }

    public Self deviceId(String deviceId) {
        this.deviceId = deviceId;

        return castSelf();
    }

    @Override
    public Self success() {
        success = true;
        return castSelf();
    }

    @Override
    public Self success(boolean success) {
        this.success = success;
        return castSelf();
    }

    public Self error(Throwable e) {
        DeviceMessageReply.super.error(e);
        return (castSelf());
    }

    @Override
    public Self error(ErrorCode errorCode) {
        return error(errorCode.name(), errorCode.getText());
    }

    @Override
    public Self error(String errorCode, String msg) {
        success = false;
        code = errorCode;
        message = msg;
        return castSelf();
    }

    @Override
    public Self from(Message message) {
        this.messageId = message.getMessageId();
        if (message instanceof DeviceMessage) {
            this.deviceId = ((DeviceMessage) message).getDeviceId();
        }

        return castSelf();
    }

    @Override
    public Self messageId(String messageId) {
        this.messageId = messageId;
        return castSelf();
    }

    @Override
    public Self timestamp(long timestamp) {
        this.timestamp = timestamp;
        return castSelf();
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, JSONObject::new);
        json.put("messageType", getMessageType().name());
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        FastBeanCopier.copy(jsonObject, this, "headers");
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
        JSONObject headers = jsonObject.getJSONObject("headers");
        if (null != headers) {
            headers.forEach(this::addHeader);
        }
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

    @Override
    public Self copy() {
        return (Self) DeviceMessageReply.super.copy();
    }

}
