package org.jetlinks.core.message;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.GenericHeaderSupport;

@Getter
@Setter
public class DefaultBroadcastMessage extends GenericHeaderSupport<DefaultBroadcastMessage> implements BroadcastMessage {
    private static final long serialVersionUID = -6849794470754667710L;

    private String messageId;

    private long timestamp = System.currentTimeMillis();

    private String address;

    private Message message;

    @Override
    public BroadcastMessage message(Message message) {
        this.message = message;
        return this;
    }

    @Override
    public BroadcastMessage address(String address) {
        this.address = address;
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
        FastBeanCopier.copy(jsonObject, this, "headers");
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
        JSONObject headers = jsonObject.getJSONObject("headers");
        if (null != headers) {
            headers.forEach(this::addHeader);
        }
    }
}
