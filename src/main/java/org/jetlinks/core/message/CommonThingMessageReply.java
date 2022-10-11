package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.GenericHeaderSupport;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;

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
public class CommonThingMessageReply<SELF extends CommonThingMessageReply<SELF>> extends GenericHeaderSupport<SELF> implements ThingMessageReply {
    private static final long serialVersionUID = -6849794470754667710L;

    private boolean success = true;

    private String code;

    private String message;

    private String messageId;

    private String thingType;

    private String thingId;

    private long timestamp = System.currentTimeMillis();

    @Override
    public ThingMessageReply thingId(String type, String thingId) {
        this.thingType = type;
        this.thingId = thingId;
        return this;
    }

    public SELF code(String code) {
        this.code = code;

        return castSelf();
    }

    public SELF message(String message) {
        this.message = message;

        return castSelf();
    }

    public SELF thingId(String thingId) {
        this.thingId = thingId;
        return castSelf();
    }

    @Override
    public SELF success() {
        success = true;
        return castSelf();
    }

    @Override
    public SELF success(boolean success) {
        this.success = success;
        return castSelf();
    }

    public SELF error(Throwable e) {
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
    public SELF error(ErrorCode errorCode) {
        success = false;
        code = errorCode.name();
        message = errorCode.getText();
        timestamp = System.currentTimeMillis();
        return castSelf();
    }

    @Override
    public SELF from(Message message) {
        this.messageId = message.getMessageId();
        if (message instanceof ThingMessage) {
            this.thingId = ((ThingMessage) message).getThingId();
            this.thingType = ((ThingMessage) message).getThingType();
        }
        return castSelf();
    }

    @Override
    public SELF messageId(String messageId) {
        this.messageId = messageId;
        return castSelf();
    }

    @Override
    public SELF timestamp(long timestamp) {
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
    @SuppressWarnings("all")
    public SELF copy() {
        return (SELF) ThingMessageReply.super.copy();
    }
}
