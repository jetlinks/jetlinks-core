package org.jetlinks.core.message.property;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReadPropertyMessage extends CommonDeviceMessage implements RepayableDeviceMessage<ReadPropertyMessageReply> {

    private List<String> properties = new ArrayList<>();

    public void addProperties(List<String> properties) {
        this.properties.addAll(properties);
    }

    @Override
    public ReadPropertyMessageReply newReply() {
        return new ReadPropertyMessageReply()
                .from(this);
    }

    public MessageType getMessageType() {
        return MessageType.READ_PROPERTY;
    }

}
