package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * @author zhouhao
 * @since 1.1.9
 */
@Getter
@Setter
public abstract class CommonThingMessage<SELF extends CommonThingMessage<SELF>> implements ThingMessage {
    private static final long serialVersionUID = -6849794470754667710L;

    private String code;

    private String messageId;

    @Nonnull
    private String thingType;

    @Nonnull
    private String thingId;

    private Map<String, Object> headers;

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

    @Override
    public synchronized SELF addHeader(String header, Object value) {
        if (headers == null) {
            this.headers = new ConcurrentHashMap<>();
        }
        if (header != null && value != null) {
            this.headers.put(header, value);
        }
        return castSelf();
    }

    @Override
    public synchronized SELF addHeaderIfAbsent(String header, Object value) {
        if (headers == null) {
            this.headers = new ConcurrentHashMap<>();
        }
        if (header != null && value != null) {
            this.headers.putIfAbsent(header, value);
        }
        return castSelf();
    }

    private Map<String, Object> safeGetHeader() {
        return headers == null ? headers = new ConcurrentHashMap<>() : headers;
    }

    public SELF messageId(String messageId) {
        this.setMessageId(messageId);
        return castSelf();
    }

    @SuppressWarnings("all")
    protected SELF castSelf() {
        return (SELF) this;
    }

    @Override
    public SELF removeHeader(String header) {
        if (this.headers != null) {
            this.headers.remove(header);
        }
        return castSelf();
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, new JSONObject());
        json.put("messageType", getMessageType().name());
        return json;
    }

    @Override
    public Object computeHeader(String key, BiFunction<String, Object, Object> computer) {
       return safeGetHeader().compute(key, computer);
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        ThingMessage.super.fromJson(jsonObject);
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
