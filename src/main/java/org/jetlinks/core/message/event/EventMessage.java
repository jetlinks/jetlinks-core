package org.jetlinks.core.message.event;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class EventMessage extends CommonDeviceMessageReply<EventMessage> {

    private String event;

    private Object data;

    public MessageType getMessageType() {
        return MessageType.EVENT;
    }

}
