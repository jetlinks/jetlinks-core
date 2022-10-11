package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.GenericHeaderSupport;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class CommonDeviceMessage<SELF extends CommonDeviceMessage<SELF> > extends GenericHeaderSupport<SELF> implements DeviceMessage {
    private static final long serialVersionUID = -6849794470754667710L;

    private String code;

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
    @JsonIgnore
    @JSONField(serialize = false)
    public final String getThingType() {
        return DeviceMessage.super.getThingType();
    }

    @Override
    public SELF messageId(String messageId) {
        setMessageId(messageId);
        return castSelf();
    }

    @Override
    public SELF thingId(String thingType, String thingId) {
        this.setDeviceId(thingId);
        return castSelf();
    }


    @Override
    public SELF timestamp(long timestamp) {
        this.timestamp = timestamp;
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

}
