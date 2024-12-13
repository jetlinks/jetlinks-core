package org.jetlinks.core.message.module;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;

/**
 * 设备模块消息
 *
 * @author zhouhao
 * @since 1.2.2
 * @see ThingModuleMessage
 */
@Getter
@Setter
public class DeviceModuleMessage extends CommonDeviceMessage<DeviceModuleMessage>
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
    public DeviceModuleMessage module(String module) {
        this.module = module;
        return this;
    }

    @Override
    public Message getMessage() {
        return message;
    }

    @Override
    public DeviceModuleMessage message(Message message) {
        this.message = message;
        return this;
    }
}
