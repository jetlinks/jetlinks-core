package org.jetlinks.core.message.event;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonThingMessage;
import org.jetlinks.core.message.MessageType;

@Getter
@Setter
public class DefaultEventMessage extends CommonThingMessage<DefaultEventMessage> implements ThingEventMessage {

    private String event;

    private Object data;

    public MessageType getMessageType() {
        return MessageType.EVENT;
    }

    @Override
    public DefaultEventMessage event(String event) {
        this.event = event;
        return this;
    }

    @Override
    public DefaultEventMessage data(Object data) {
        this.data = data;
        return this;
    }
}
