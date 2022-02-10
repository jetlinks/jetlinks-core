package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

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
public class CommonDeviceMessageReply<Self extends CommonDeviceMessageReply> implements DeviceMessageReply {
    private static final long serialVersionUID = -6849794470754667710L;

    private boolean success = true;

    private String code;

    private String message;

    private String messageId;

    private String deviceId;

    private long timestamp = System.currentTimeMillis();

    private Map<String, Object> headers;

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

    private Map<String, Object> safeGetHeader() {
        return headers == null ? headers = new ConcurrentHashMap<>(64) : headers;
    }

    @Override
    public synchronized Self addHeaderIfAbsent(String header, Object value) {
        if (header != null && value != null) {
            safeGetHeader().putIfAbsent(header, value);
        }
        return caseSelf();
    }

    @Override
    public synchronized Self addHeader(String header, Object value) {
        if (header != null && value != null) {
            safeGetHeader().put(header, value);
        }
        return caseSelf();
    }

    @Override
    public Self removeHeader(String header) {
        if (headers != null) {
            this.headers.remove(header);
        }
        return caseSelf();
    }

    public Self code(String code) {
        this.code = code;

        return caseSelf();
    }

    public Self message(String message) {
        this.message = message;

        return caseSelf();
    }

    public Self deviceId(String deviceId) {
        this.deviceId = deviceId;

        return caseSelf();
    }

    @Override
    public Self success() {
        success = true;
        return caseSelf();
    }

    @Override
    public Self success(boolean success) {
        this.success = success;
        return caseSelf();
    }

    public Self error(Throwable e) {
        success = false;
        if (e instanceof DeviceOperationException) {
            error(((DeviceOperationException) e).getCode());
        } else {
            error(ErrorCode.SYSTEM_ERROR);
        }
        setMessage(e.getMessage());
        addHeader("errorType", e.getClass().getName());
        addHeader("errorMessage", e.getMessage());

        return (caseSelf());
    }

    @Override
    public Self error(ErrorCode errorCode) {
        success = false;
        code = errorCode.name();
        message = errorCode.getText();
        timestamp = System.currentTimeMillis();
        return caseSelf();
    }

    @Override
    public Self from(Message message) {
        this.messageId = message.getMessageId();
        if (message instanceof DeviceMessage) {
            this.deviceId = ((DeviceMessage) message).getDeviceId();
        }

        return caseSelf();
    }

    @Override
    public Self messageId(String messageId) {
        this.messageId = messageId;
        return caseSelf();
    }

    @Override
    public Self timestamp(long timestamp) {
        this.timestamp = timestamp;
        return caseSelf();
    }

    @Override
    public <T> Self addHeader(HeaderKey<T> header, T value) {
        return (Self) DeviceMessageReply.super.addHeader(header, value);
    }

    private Self caseSelf() {
        return (Self) this;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, JSONObject::new);
        json.put("messageType", getMessageType().name());
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        DeviceMessageReply.super.fromJson(jsonObject);
        success = jsonObject.getBooleanValue("success");

        timestamp = jsonObject.getLongValue("timestamp");
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
        messageId = jsonObject.getString("messageId");
        deviceId = jsonObject.getString("deviceId");
        if (deviceId == null) {
            deviceId = jsonObject.getString("thingId");
        }
        code = jsonObject.getString("code");
        message = jsonObject.getString("message");
        headers = jsonObject.getJSONObject("headers");
    }

    @Override
    public Object computeHeader(String key, BiFunction<String, Object, Object> computer) {
        return safeGetHeader().compute(key, computer);
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
