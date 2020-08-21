package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.enums.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;

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
public class CommonDeviceMessageReply<ME extends CommonDeviceMessageReply> implements DeviceMessageReply {
    private static final long serialVersionUID = -6849794470754667710L;

    private boolean success;

    private String code;

    private String message;

    private String messageId;

    private String deviceId;

    private long timestamp = System.currentTimeMillis();

    private Map<String, Object> headers;

    @Override
    public ME addHeaderIfAbsent(String header, Object value) {
        if (headers == null) {
            this.headers = new LinkedHashMap<>();
        }
        this.headers.putIfAbsent(header, value);
        return (ME) this;
    }

    @Override
    public ME addHeader(String header, Object value) {
        if (headers == null) {
            this.headers = new LinkedHashMap<>();
        }
        this.headers.put(header, value);
        return (ME) this;
    }

    @Override
    public ME removeHeader(String header) {
        if (headers != null) {
            this.headers.remove(header);
        }
        return (ME) this;
    }

    public ME code(String code) {
        this.code = code;

        return (ME) this;
    }

    public ME message(String message) {
        this.message = message;

        return (ME) this;
    }

    public ME deviceId(String deviceId) {
        this.deviceId = deviceId;

        return (ME) this;
    }

    @Override
    public ME success() {
        success = true;
        return (ME) this;
    }

    public ME error(Throwable e) {
        success = false;
        error(ErrorCode.SYSTEM_ERROR);
        addHeader("errorType", e.getClass().getName());
        addHeader("errorMessage", e.getMessage());

        return ((ME) this);
    }

    @Override
    public ME error(ErrorCode errorCode) {
        success = false;
        code = errorCode.name();
        message = errorCode.getText();
        timestamp = System.currentTimeMillis();
        return (ME) this;
    }

    @Override
    public ME from(Message message) {
        this.messageId = message.getMessageId();
        if (message instanceof DeviceMessage) {
            this.deviceId = ((DeviceMessage) message).getDeviceId();
        }

        return (ME) this;
    }

    @Override
    public ME messageId(String messageId) {
        this.messageId = messageId;
        return (ME) this;
    }

    @Override
    public <T> ME addHeader(HeaderKey<T> header, T value) {
        return (ME) DeviceMessageReply.super.addHeader(header, value);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this,JSONObject::new);
        json.put("messageType",getMessageType().name());
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        success = jsonObject.getBoolean("success");
        timestamp = jsonObject.getLong("timestamp");
        messageId = jsonObject.getString("messageId");
        deviceId = jsonObject.getString("deviceId");
        code = jsonObject.getString("code");
        message = jsonObject.getString("message");
        headers = jsonObject.getJSONObject("headers");
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

}
