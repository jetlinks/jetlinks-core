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
    public ThingMessage thingId(String thingType, String thingId) {
        this.setThingType(thingType);
        this.setThingId(thingId);
        return this;
    }

    @Override
    public synchronized ThingMessage addHeader(String header, Object value) {
        if (headers == null) {
            this.headers = new ConcurrentHashMap<>();
        }
        if (header != null && value != null) {
            this.headers.put(header, value);
        }
        return this;
    }

    @Override
    public synchronized ThingMessage addHeaderIfAbsent(String header, Object value) {
        if (headers == null) {
            this.headers = new ConcurrentHashMap<>();
        }
        if (header != null && value != null) {
            this.headers.putIfAbsent(header, value);
        }
        return this;
    }

    private Map<String, Object> safeGetHeader() {
        return headers == null ? headers = new ConcurrentHashMap<>() : headers;
    }

    public SELF messageId(String messageId){
        this.setMessageId(messageId);
        return castSelf();
    }

    @SuppressWarnings("all")
    protected SELF castSelf(){
        return (SELF)this;
    }

    @Override
    public ThingMessage removeHeader(String header) {
        if (this.headers != null) {
            this.headers.remove(header);
        }
        return this;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, new JSONObject());
        json.put("messageType", getMessageType().name());
        return json;
    }

    @Override
    public void computeHeader(String key, BiFunction<String, Object, Object> computer) {
        safeGetHeader().compute(key, computer);
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
        return (SELF)ThingMessage.super.copy();
    }
}
