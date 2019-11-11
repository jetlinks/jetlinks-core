package org.jetlinks.core.message.property;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class WritePropertyMessage extends CommonDeviceMessage implements RepayableDeviceMessage<WritePropertyMessageReply> {

    private Map<String, Object> properties = new LinkedHashMap<>();

    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    @Override
    public WritePropertyMessageReply newReply() {
        return new WritePropertyMessageReply().from(this);
    }

    public MessageType getMessageType() {
        return MessageType.WRITE_PROPERTY;
    }


}
