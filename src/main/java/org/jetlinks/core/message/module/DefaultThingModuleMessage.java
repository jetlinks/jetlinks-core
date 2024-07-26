package org.jetlinks.core.message.module;

import org.jetlinks.core.message.CommonThingMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;

public class DefaultThingModuleMessage extends CommonThingMessage<DefaultThingModuleMessage>
    implements ThingModuleMessage {

    private String module;
    private Message message;

    @Override
    public final MessageType getMessageType() {
        return MessageType.MODULE;
    }

    @Override
    public String getModule() {
        return module;
    }

    @Override
    public DefaultThingModuleMessage module(String module) {
        this.module = module;
        return this;
    }

    @Override
    public Message getMessage() {
        return message;
    }

    @Override
    public DefaultThingModuleMessage message(Message message) {
        this.message = message;
        return this;
    }
}
