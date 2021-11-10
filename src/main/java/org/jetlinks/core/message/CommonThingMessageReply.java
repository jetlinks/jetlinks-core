package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
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
public class CommonThingMessageReply<ME extends CommonThingMessageReply<ME>> implements ThingMessageReply {
    private static final long serialVersionUID = -6849794470754667710L;

    private boolean success = true;

    private String code;

    private String message;

    private String messageId;

    private String thingType;

    private String thingId;

    private long timestamp = System.currentTimeMillis();

    private Map<String, Object> headers;


    private Map<String, Object> safeGetHeader() {
        return headers == null ? headers = new ConcurrentHashMap<>() : headers;
    }

    @Override
    public ThingMessageReply thingId(String type, String thingId) {
        this.thingType = type;
        this.thingId = thingId;
        return this;
    }

    @Override
    public synchronized ME addHeaderIfAbsent(String header, Object value) {
        if (header != null && value != null) {
            safeGetHeader().putIfAbsent(header, value);
        }
        return castSelf();
    }

    @Override
    public synchronized ME addHeader(String header, Object value) {
        if (header != null && value != null) {
            safeGetHeader().put(header, value);
        }
        return castSelf();
    }

    @Override
    public ME removeHeader(String header) {
        if (headers != null) {
            this.headers.remove(header);
        }
        return castSelf();
    }

    public ME code(String code) {
        this.code = code;

        return castSelf();
    }

    public ME message(String message) {
        this.message = message;

        return castSelf();
    }

    public ME thingId(String thingId) {
        this.thingId = thingId;
        return castSelf();
    }

    @Override
    public ME success() {
        success = true;
        return castSelf();
    }

    public ME error(Throwable e) {
        success = false;
        if (e instanceof DeviceOperationException) {
            error(((DeviceOperationException) e).getCode());
        } else {
            error(ErrorCode.SYSTEM_ERROR);
        }
        setMessage(e.getMessage());
        addHeader("errorType", e.getClass().getName());
        addHeader("errorMessage", e.getMessage());

        return castSelf();
    }

    @Override
    public ME error(ErrorCode errorCode) {
        success = false;
        code = errorCode.name();
        message = errorCode.getText();
        timestamp = System.currentTimeMillis();
        return castSelf();
    }

    @Override
    public ME from(Message message) {
        this.messageId = message.getMessageId();
        if (message instanceof ThingMessage) {
            this.thingId = ((ThingMessage) message).getThingId();
            this.thingType = ((ThingMessage) message).getThingType();
        }
        return castSelf();
    }

    @Override
    public ME messageId(String messageId) {
        this.messageId = messageId;
        return castSelf();
    }

    @Override
    public <T> ME addHeader(HeaderKey<T> header, T value) {
        return (ME) ThingMessageReply.super.addHeader(header, value);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, JSONObject::new);
        json.put("messageType", getMessageType().name());
        return json;
    }

    @SuppressWarnings("all")
    protected ME castSelf() {
        return (ME) this;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        ThingMessageReply.super.fromJson(jsonObject);
        success = jsonObject.getBooleanValue("success");

        timestamp = jsonObject.getLongValue("timestamp");
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
        messageId = jsonObject.getString("messageId");
        thingType = jsonObject.getString("thingType");
        thingId = jsonObject.getString("thingId");
        code = jsonObject.getString("code");
        message = jsonObject.getString("message");
        headers = jsonObject.getJSONObject("headers");
    }

    @Override
    public void computeHeader(String key, BiFunction<String, Object, Object> computer) {
        safeGetHeader().compute(key, computer);
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

}
