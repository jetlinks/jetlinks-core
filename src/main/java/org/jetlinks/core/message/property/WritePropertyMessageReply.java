package org.jetlinks.core.message.property;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class WritePropertyMessageReply extends CommonDeviceMessageReply<WritePropertyMessageReply> {

    private Map<String, Object> properties;

    public synchronized WritePropertyMessageReply addProperty(String key, Object value) {
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        properties.put(key, value);
        return this;
    }


    public static WritePropertyMessageReply create() {
        WritePropertyMessageReply reply = new WritePropertyMessageReply();

        reply.setTimestamp(System.currentTimeMillis());

        return reply;
    }

    public Optional<Object> getProperty(String property) {
        return Optional
                .ofNullable(properties)
                .map(map -> map.get(property));
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.properties = jsonObject.getJSONObject("properties");
    }

    public MessageType getMessageType() {
        return MessageType.WRITE_PROPERTY_REPLY;
    }

}
