package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReadPropertyMessageReply extends CommonDeviceMessageReply<ReadPropertyMessageReply> {

    private Map<String, Object> properties;

    public static ReadPropertyMessageReply create() {
        ReadPropertyMessageReply reply = new ReadPropertyMessageReply();

        reply.setTimestamp(System.currentTimeMillis());

        return reply;
    }

    public ReadPropertyMessageReply success(Map<String, Object> properties) {

        this.properties = properties;
        super.setSuccess(true);
        return this;

    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.properties = jsonObject.getJSONObject("properties");
    }

    public MessageType getMessageType() {
        return MessageType.READ_PROPERTY_REPLY;
    }

}
