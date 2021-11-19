package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

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

    private Map<String, Object> headers;

    private long timestamp = System.currentTimeMillis();

    @Override
    @JsonIgnore
    @JSONField(serialize = false)
    public final String getThingId() {
        return getDeviceId();
    }

    @Override
    @JsonIgnore
    @JSONField(serialize = false)
    public final String getThingType() {
        return DeviceMessage.super.getThingType();
    }

    @Override
    public CommonDeviceMessage messageId(String messageId) {
        setMessageId(messageId);
        return this;
    }

    @Override
    public CommonDeviceMessage thingId(String thingType, String thingId) {
        this.setDeviceId(thingId);
        return this;
    }

    private Map<String, Object> safeGetHeader() {
        return headers == null ? headers = new ConcurrentHashMap<>() : headers;
    }

    @Override
    public CommonDeviceMessage timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Override
    public synchronized CommonDeviceMessage addHeader(String header, Object value) {

        if (header != null && value != null) {
            safeGetHeader().put(header, value);
        }
        return this;
    }

    @Override
    public synchronized CommonDeviceMessage addHeaderIfAbsent(String header, Object value) {

        if (header != null && value != null) {
            safeGetHeader().putIfAbsent(header, value);
        }
        return this;
    }

    @Override
    public CommonDeviceMessage removeHeader(String header) {
        if (this.headers != null) {
            this.headers.remove(header);
        }
        return this;
    }

    @Override
    public Object computeHeader(String key, BiFunction<String, Object, Object> computer) {
       return safeGetHeader().compute(key, computer);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, new JSONObject());
        json.put("messageType", getMessageType().name());
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        DeviceMessage.super.fromJson(jsonObject);
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

}
