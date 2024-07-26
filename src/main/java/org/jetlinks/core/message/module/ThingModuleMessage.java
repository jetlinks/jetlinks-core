package org.jetlinks.core.message.module;

import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 针对模块的消息
 *
 * @author zhouhao
 * @see DeviceModuleMessage
 * @see DefaultThingModuleMessage
 * @see ThingMetadata#getModules()
 * @since 1.2.2
 */
public interface ThingModuleMessage extends ThingMessage {
    /**
     * 模块ID
     *
     * @return 模块ID
     */
    String getModule();

    /**
     * 设置模块ID
     *
     * @param module 模块ID
     * @return this
     */
    ThingModuleMessage module(String module);

    /**
     * 模块消息
     *
     * @return 模块消息
     */
    Message getMessage();

    /**
     * 设置模块消息
     *
     * @param message 模块消息
     * @return this
     */
    ThingModuleMessage message(Message message);

    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ThingMessage.super.readExternal(in);
        module(SerializeUtils.readNullableUTF(in));
        message((Message) SerializeUtils.readObject(in));
    }

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        ThingMessage.super.writeExternal(out);
        SerializeUtils.writeNullableUTF(getModule(), out);
        SerializeUtils.writeObject(getMessage(), out);
    }
}
