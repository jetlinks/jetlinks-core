package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.GenericHeaderSupport;

import javax.annotation.Nonnull;

/**
 * @author zhouhao
 * @since 1.1.9
 */
@Getter
@Setter
public abstract class CommonThingMessage<SELF extends CommonThingMessage<SELF>> extends GenericHeaderSupport<SELF> implements ThingMessage {
    private static final long serialVersionUID = -6849794470754667710L;

    private String code;

    private String messageId;

    @Nonnull
    private String thingType;

    @Nonnull
    private String thingId;

    private long timestamp = System.currentTimeMillis();

    public abstract MessageType getMessageType();

    @Override
    public SELF thingId(String thingType, String thingId) {
        this.setThingType(thingType);
        this.setThingId(thingId);
        return castSelf();
    }

    @Override
    public SELF timestamp(long timestamp) {
        this.timestamp = timestamp;
        return castSelf();
    }


    public SELF messageId(String messageId) {
        this.setMessageId(messageId);
        return castSelf();
    }


    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, new JSONObject());
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
        return (SELF) ThingMessage.super.copy();
    }

}
