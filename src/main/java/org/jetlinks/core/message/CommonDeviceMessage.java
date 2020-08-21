package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public DeviceMessage addHeader(String header, Object value) {
        if (headers == null) {
            this.headers = new LinkedHashMap<>();
        }
        this.headers.put(header, value);
        return this;
    }

    @Override
    public DeviceMessage addHeaderIfAbsent(String header, Object value) {
        if (headers == null) {
            this.headers = new LinkedHashMap<>();
        }
        this.headers.putIfAbsent(header, value);
        return this;
    }

    @Override
    public DeviceMessage removeHeader(String header) {
        if (this.headers != null) {
            this.headers.remove(header);
        }
        return this;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = FastBeanCopier.copy(this, JSONObject::new);
        json.put("messageType", getMessageType().name());
        return json;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        this.deviceId = jsonObject.getString("deviceId");
        this.messageId = jsonObject.getString("messageId");
        this.code = jsonObject.getString("code");
        this.headers = jsonObject.getJSONObject("headers");
    }

    @Override
    public String toString() {
        return toJson().toJSONString();
    }

}
