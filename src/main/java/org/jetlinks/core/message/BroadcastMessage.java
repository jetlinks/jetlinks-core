package org.jetlinks.core.message;

import org.jetlinks.core.utils.SerializeUtils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 广播消息
 */
public interface BroadcastMessage extends Message {

    String getAddress();

    Message getMessage();

    BroadcastMessage address(String address);

    BroadcastMessage message(Message message);

    default MessageType getMessageType() {
        return MessageType.BROADCAST;
    }

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        SerializeUtils.writeNullableUTF(getAddress(), out);
        MessageType.writeExternal(getMessage(), out);
    }

    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        address(SerializeUtils.readNullableUTF(in));
        message(MessageType.readExternal(in));

    }
}
